#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# EXP_IDS
# SPECIES
source ${SCRIPT_DIR}/../test-data.env
# ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME
# ATLAS_DATA_SCXA_VOL_NAME
# SOLR_CLOUD_CONTAINER_1_NAME
# SOLR_CLOUD_CONTAINER_2_NAME
source ${SCRIPT_DIR}/../../dev.env
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n' "Usage: ${0} [ -k DIRECTORY ] [ -o FILE ] [ -l FILE ]"
  printf '\n%b\n' "Populate a Docker Compose SolrCloud 8 cluster with Single Cell Expression Atlas data."

  printf '\n%b\n' "-k DIRECTORY\tDirectory where RSA private/public keypair"
  printf '%b\n' "\t\tfiles will be written (defaults to the working directory)"

  printf '%b\n' "-o FILE\t\tPath to scatlas.owl in the host machine; if this"
  printf '%b\n' "\t\toption is not provided it will download it from"
  printf '%b\n' "\t\thttps://github.com/EBISPOT/scatlas_ontology"

  printf '\n%b\n' "-l FILE \tLog file (default is /dev/stdout)"
  printf '%b\n\n' "-h\t\tDisplay usage instructions"
}

SOLR_KEYS_DIRECTORY=${SCRIPT_DIR}
LOG_FILE=/dev/stdout
while getopts "k:o:l:h" opt
do
  case ${opt} in
    k)
      SOLR_KEYS_DIRECTORY=${OPTARG}
      ;;
    o)
      SC_ATLAS_ONTOLOGY_FILE=${OPTARG}
      ;;
    l)
      LOG_FILE=$OPTARG
      ;;
    h)
      print_usage
      exit 0
      ;;
    \?)
      printf '%b\n' "Invalid option: -${OPTARG}" >&2
      print_usage
      exit 2
      ;;
  esac
done

IMAGE_NAME=scxa-solr-indexer
print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build \
-t ${IMAGE_NAME} ${SCRIPT_DIR} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🔐 Generate RSA keypair to sign and verify Solr packages"
SOLR_PRIVATE_KEY=${SOLR_KEYS_DIRECTORY}/scxa-solrcloud.pem
SOLR_PUBLIC_KEY=${SOLR_KEYS_DIRECTORY}/scxa-solrcloud.der
openssl genrsa -out ${SOLR_PRIVATE_KEY} 512 >> ${LOG_FILE} 2>&1
openssl rsa -in ${SOLR_PRIVATE_KEY} -pubout -outform DER -out ${SOLR_PUBLIC_KEY} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🌅 Start Solr 8 cluster in Docker Compose"
SOLR_PUBLIC_KEY=${SOLR_PUBLIC_KEY} \
docker-compose \
--env-file ${SCRIPT_DIR}/../../dev.env \
-f ${SCRIPT_DIR}/../../docker-compose-solrcloud.yml \
up -d >> ${LOG_FILE} 2>&1
print_done

print_stage_name "💤 Give Solr ten seconds to start up before copying ontology file..."
sleep 10
print_done

if [ -z ${SC_ATLAS_ONTOLOGY_FILE+x} ]
then
  SC_ATLAS_ONTOLOGY_FILE=${SCRIPT_DIR}/scatlas.owl
  print_stage_name "🌐 No OWL file provided, download https://github.com/EBISPOT/scatlas_ontology/raw/master/scatlas.owl"
  curl -o ${SC_ATLAS_ONTOLOGY_FILE} -H 'Accept: application/vnd.github.v3.raw' https://api.github.com/repos/EBISPOT/scatlas_ontology/contents/scatlas.owl >> ${LOG_FILE} 2>&1
  print_done
fi

SOLR_USERFILES_PATH=/var/solr/data/userfiles/
print_stage_name "📑 Copy ${SC_ATLAS_ONTOLOGY_FILE} to ${SOLR_CLOUD_CONTAINER_1_NAME}:${SOLR_USERFILES_PATH}"
docker cp ${SC_ATLAS_ONTOLOGY_FILE} ${SOLR_CLOUD_CONTAINER_1_NAME}:${SOLR_USERFILES_PATH} >> ${LOG_FILE} 2>&1
print_done
print_stage_name "📑 Copy ${SC_ATLAS_ONTOLOGY_FILE} to ${SOLR_CLOUD_CONTAINER_2_NAME}:${SOLR_USERFILES_PATH}"
docker cp ${SC_ATLAS_ONTOLOGY_FILE} ${SOLR_CLOUD_CONTAINER_2_NAME}:${SOLR_USERFILES_PATH} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🔏 Register ${SOLR_PUBLIC_KEY} in SolrCloud"
docker exec ${SOLR_CLOUD_CONTAINER_1_NAME} ./bin/solr package add-key /run/secrets/scxa-solrcloud.der >> ${LOG_FILE} 2>&1
print_done

POSTGRES_HOST=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=pgpass
print_stage_name "⚙ Spin up containers to index volume data in Solr"
echo "docker run --network atlas-test-net -h ${POSTGRES_HOST} -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} -d postgres:11-alpine"
PG_CONTAINER_ID=$(docker run --network atlas-test-net -h ${POSTGRES_HOST} -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} -d postgres:11-alpine)

GRADLE_RO_DEP_CACHE_DEST=/gradle-ro-dep-cache
SOLR_PRIVATE_KEY_DEST=/run/secrets/$(basename ${SOLR_PRIVATE_KEY})
docker run --rm -it \
-v ${SOLR_PRIVATE_KEY}:${SOLR_PRIVATE_KEY_DEST}:ro \
-v ${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME}:/atlas-data/bioentity_properties:ro \
-v ${ATLAS_DATA_SCXA_VOL_NAME}:/atlas-data/scxa:ro \
-v ${GRADLE_RO_DEP_CACHE_VOL_NAME}:${GRADLE_RO_DEP_CACHE_DEST}:ro \
-e GRADLE_RO_DEP_CACHE=${GRADLE_RO_DEP_CACHE_DEST} \
-e POSTGRES_HOST=${POSTGRES_HOST} \
-e POSTGRES_USER=${POSTGRES_USER} \
-e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
-e SPECIES="${SPECIES}" \
-e EXP_IDS="${EXP_IDS}" \
-e SOLR_HOST=${SOLR_CLOUD_CONTAINER_1_NAME}:8983 \
-e SOLR_NUM_SHARDS=2 \
-e NUM_DOCS_PER_BATCH=20000 \
-e SOLR_COLLECTION_BIOENTITIES=bioentities \
-e SOLR_COLLECTION_BIOENTITIES_SCHEMA_VERSION=1 \
-e BIOSOLR_VERSION=2.0.0 \
-e BIOSOLR_JAR_PATH=/root/index-scxa/lib/solr-ontology-update-processor-2.0.0.jar \
-e SIGNING_PRIVATE_KEY=${SOLR_PRIVATE_KEY_DEST} \
-e SCXA_ONTOLOGY=file://${SOLR_USERFILES_PATH}/scatlas.owl \
--network atlas-test-net \
${IMAGE_NAME} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🧹 Clean up Postgres container"
echo "docker stop ${PG_CONTAINER_ID}"
docker stop ${PG_CONTAINER_ID} >> ${LOG_FILE} 2>&1
docker rm ${PG_CONTAINER_ID} >> ${LOG_FILE} 2>&1
print_done

printf '%b\n' "🙂 All done! Point your browser at http://localhost:8983 to explore your SolrCloud instance."
printf '%b\n' "ℹ You can keep $(basename ${SOLR_PRIVATE_KEY}) and reuse it to sign any other Solr packages."
