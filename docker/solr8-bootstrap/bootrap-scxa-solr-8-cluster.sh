#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

function print_done() {
  printf "✅\n\n"
}

function print_error() {
  printf "\n\n😢 Something went wrong! See ${LOG_FILE} for more details.\n"
}
trap print_error ERR

function print_usage() {
  printf "Usage: $0 [ -k DIRECTORY ] [ -o FILE ] [ -l FILE ]\n"
  printf "Populate a Docker Compose SolrCloud 8 cluster with Single Cell Expression Atlas data.\n\n"

  printf "-k DIRECTORY \tDirectory where RSA private/public keypair\n"
  printf "\t\tfiles will be written (defaults to the working directory)\n"

  printf "-o FILE \tPath to scatlas.owl in the host machine; if this\n"
  printf "\t\toption is not provided it will download it from\n"
  printf "\t\thttps://github.com/EBISPOT/scatlas_ontology\n"

  printf "-l FILE \tLog file (default is log.out)\n"

  printf "-h\t\tDisplay usage instructions\n"
}

SOLR_KEYS_DIRECTORY=${SCRIPT_DIR}
LOG_FILE=/dev/stdout
while getopts "k:o:l:h" opt
do
  case $opt in
    k)
      SOLR_KEYS_DIRECTORY=$OPTARG
      ;;
    o)
      SC_ATLAS_ONTOLOGY_FILE=$OPTARG
      ;;
    l)
      LOG_FILE=$OPTARG
      ;;
    h)
      print_usage
      exit 0
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo ""
      print_usage
      exit 1
      ;;
  esac
done

[ ! -r ${SC_ATLAS_ONTOLOGY_FILE} ] && printf "🛑  ${SC_ATLAS_ONTOLOGY_FILE} does not exist or is not readable.\n\n" && print_usage && exit 1
[ ! -w ${SOLR_KEYS_DIRECTORY} ] && printf "🛑  ${SOLR_KEYS_DIRECTORY} does not exist or is not writable.\n\n" && print_usage && exit 1

printf "🚧 Build Docker images "
docker build -t scxa-volumes-populator ./scxa-volumes-populator &>> ${LOG_FILE}
docker build -t scxa-solrcloud-8-indexer ./scxa-solrcloud-8-indexer &>> ${LOG_FILE}
print_done

BIOENTITY_PROPERTIES_VOL_NAME=bioentity-properties
SCXA_DATA_VOL_NAME=scxa-data
printf "💾 Create Docker volumes ${BIOENTITY_PROPERTIES_VOL_NAME} and ${SCXA_DATA_VOL_NAME} "
docker volume create ${BIOENTITY_PROPERTIES_VOL_NAME} &>> ${LOG_FILE}
docker volume create ${SCXA_DATA_VOL_NAME} &>> ${LOG_FILE}
print_done

printf "⚙ Spin up ephemeral container to populate volumes from EBI’s FTP server "
docker run --rm -v ${BIOENTITY_PROPERTIES_VOL_NAME}:/bioentity_properties -v ${SCXA_DATA_VOL_NAME}:/scxa-data scxa-volumes-populator &>> ${LOG_FILE}
print_done

printf "🔐 Generate RSA keypair to sign and verify Solr packages "
SOLR_PRIVATE_KEY=${SOLR_KEYS_DIRECTORY}/scxa-solrcloud.pem
SOLR_PUBLIC_KEY=${SOLR_KEYS_DIRECTORY}/scxa-solrcloud.der
openssl genrsa -out ${SOLR_PRIVATE_KEY} 512 &>> ${LOG_FILE}
openssl rsa -in ${SOLR_PRIVATE_KEY} -pubout -outform DER -out ${SOLR_PUBLIC_KEY} &>> ${LOG_FILE}
print_done

#printf "🤐 Store public key as a Docker secret "
#docker secret rm scxa-solrcloud.der &>> ${LOG_FILE} || true
#docker secret create scxa-solrcloud.der ${SOLR_PUBLIC_KEY} &>> ${LOG_FILE}
#print_done

printf "🌅 Start Solr 8 cluster in Docker Compose "
SOLR_PUBLIC_KEY=${SOLR_PUBLIC_KEY} docker-compose -f ${SCRIPT_DIR}/../docker-compose-solrcloud-8.yml up -d &>> ${LOG_FILE}
print_done

printf "💤 Wait for ten seconds before copying ontology file... "
sleep 10s
print_done

if [ -z ${SC_ATLAS_ONTOLOGY_FILE+x} ]
then
  SC_ATLAS_ONTOLOGY_FILE=${PWD}/scatlas.owl
  printf "🌐 No OWL file provided, download https://github.com/EBISPOT/scatlas_ontology/raw/master/scatlas.owl "
  curl -o ${SC_ATLAS_ONTOLOGY_FILE} -H 'Accept: application/vnd.github.v3.raw' https://api.github.com/repos/EBISPOT/scatlas_ontology/contents/scatlas.owl &>> ${LOG_FILE}
  print_done
fi

SOLR_CLOUD_CONTAINER_1=solr-8.7-scxa-solrcloud-0
SOLR_CLOUD_CONTAINER_2=solr-8.7-scxa-solrcloud-1
SOLR_USERFILES_PATH=/var/solr/data/userfiles/
printf "📑 Copy ${SC_ATLAS_ONTOLOGY_FILE} to ${SOLR_CLOUD_CONTAINER_1}:${SOLR_USERFILES_PATH} "
docker cp ${SC_ATLAS_ONTOLOGY_FILE} solr-8.7-scxa-solrcloud-0:/var/solr/data/userfiles/ &>> ${LOG_FILE}
print_done
printf "📑 Copy ${SC_ATLAS_ONTOLOGY_FILE} to ${SOLR_CLOUD_CONTAINER_2}:${SOLR_USERFILES_PATH} "
docker cp ${SC_ATLAS_ONTOLOGY_FILE} solr-8.7-scxa-solrcloud-1:/var/solr/data/userfiles/ &>> ${LOG_FILE}
print_done

printf "🔏 Register ${SOLR_PUBLIC_KEY} in SolrCloud "
docker exec solr-8.7-scxa-solrcloud-0 ./bin/solr package add-key /run/secrets/scxa-solrcloud.der &>> ${LOG_FILE}
print_done

printf "⚙ Spin up containers to index volume data in Solr "
docker run --network atlas-test-net --name dummy-postgres --env POSTGRES_PASSWORD=foobar -d postgres:10-alpine &>> ${LOG_FILE}
docker run --network atlas-test-net --rm -v ${SOLR_PRIVATE_KEY}:/run/secrets/$(basename ${SOLR_PRIVATE_KEY}):ro -v bioentity-properties:/bioentity_properties:ro -v scxa-data:/scxa-data:ro scxa-solrcloud-8-indexer &>> ${LOG_FILE}
print_done

printf "🧹 Clean up Postgres container "
docker stop dummy-postgres  &>> ${LOG_FILE}
docker rm dummy-postgres &>> ${LOG_FILE}
print_done

printf "🙂 All done! Point your browser at http://localhost:8983 to explore your SolrCloud instance. 🎉\n"
printf "ℹ You can keep $(basename ${SOLR_PRIVATE_KEY}) and reuse it to sign any other Solr packages.\n"
