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
ENV_FILE=${SCRIPT_DIR}/../../dev.env
source ${ENV_FILE}

# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

SOLR_KEYS_DIRECTORY=${SCRIPT_DIR}
REMOVE_VOLUMES=false
LOG_FILE=/dev/stdout
function print_usage() {
  printf '\n%b\n' "Usage: ${0} [ -k DIRECTORY ] [ -o FILE ] [ -l FILE ]"
  printf '\n%b\n' "Populate a Docker Compose SolrCloud 8 cluster with Single Cell Expression Atlas data."

  printf '\n%b\n' "-k DIRECTORY\tDirectory where RSA private/public keypair"
  printf '%b\n' "\t\tfiles will be written (defaults to the working directory)"

  printf '%b\n' "-o FILE\t\tPath to scatlas.owl in the host machine; if this"
  printf '%b\n' "\t\toption is not provided it will download it from"
  printf '%b\n' "\t\thttps://github.com/EBISPOT/scatlas_ontology"

  printf '\n%b\n' "-r\t\tRemove volumes before creating them"
  printf '\n%b\n' "-l FILE \tLog file (default is ${LOG_FILE})"
  printf '%b\n\n' "-h\t\tDisplay usage instructions"
}


while getopts "k:o:l:rh" opt
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
    r)
      REMOVE_VOLUMES=true
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

DOCKER_COMPOSE_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
--env-file ${SCRIPT_DIR}/../test-data.env \
--file ${SCRIPT_DIR}/../../docker-compose-solrcloud.yml \
--file ${SCRIPT_DIR}/docker-compose.yml"

DOCKER_COMPOSE_SOLRCLOUD_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
--file ${SCRIPT_DIR}/../../docker-compose-solrcloud.yml"

SOLR_PRIVATE_KEY=${SOLR_KEYS_DIRECTORY}/solrcloud.pem
SOLR_PUBLIC_KEY=${SOLR_KEYS_DIRECTORY}/solrcloud.der
SOLR_USERFILES_PATH=/var/solr/data/userfiles/
DOCKER_COMPOSE_COMMAND_VARS="DOCKERFILE_PATH=${SCRIPT_DIR} SOLR_USERFILES_PATH=/var/solr/data/userfiles/ SOLR_PRIVATE_KEY_DEST=/run/secrets/$(basename ${SOLR_PRIVATE_KEY}) SOLR_PUBLIC_KEY=${SOLR_PUBLIC_KEY}"

if [ "${REMOVE_VOLUMES}" = "true" ]; then
  countdown "🗑 Remove Docker Compose Solr and ZooKeeper volumes"
  eval "${DOCKER_COMPOSE_SOLRCLOUD_COMMAND}" "down --volumes >> ${LOG_FILE} 2>&1"
  print_done
fi

print_stage_name "🔐 Generate RSA keypair to sign and verify Solr packages"
openssl genrsa -out ${SOLR_PRIVATE_KEY} 512 >> ${LOG_FILE} 2>&1
openssl rsa -in ${SOLR_PRIVATE_KEY} -pubout -outform DER -out ${SOLR_PUBLIC_KEY} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🌅 Start Solr 8 cluster in Docker Compose"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_SOLRCLOUD_COMMAND}" "up -d >> ${LOG_FILE} 2>&1"
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

print_stage_name "📑 Copy ${SC_ATLAS_ONTOLOGY_FILE} to ${SOLR_CLOUD_CONTAINER_1_NAME}:${SOLR_USERFILES_PATH}"
docker cp ${SC_ATLAS_ONTOLOGY_FILE} ${SOLR_CLOUD_CONTAINER_1_NAME}:${SOLR_USERFILES_PATH} >> ${LOG_FILE} 2>&1
print_done
print_stage_name "📑 Copy ${SC_ATLAS_ONTOLOGY_FILE} to ${SOLR_CLOUD_CONTAINER_2_NAME}:${SOLR_USERFILES_PATH}"
docker cp ${SC_ATLAS_ONTOLOGY_FILE} ${SOLR_CLOUD_CONTAINER_2_NAME}:${SOLR_USERFILES_PATH} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🔏 Register ${SOLR_PUBLIC_KEY} in SolrCloud"
docker exec ${SOLR_CLOUD_CONTAINER_1_NAME} ./bin/solr package add-key /run/secrets/solrcloud.der >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🌄 Stop Solr 8 cluster in Docker Compose"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_SOLRCLOUD_COMMAND}" "down >> ${LOG_FILE} 2>&1"
print_done

print_stage_name "🛫 Spin up containers to index bioentity annotations and test experiments metadata in Solr"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up --build --abort-on-container-exit --exit-code-from solr-populator >> ${LOG_FILE} 2>&1"
print_done

print_stage_name "🛬 Bring down all services"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --rmi local >> ${LOG_FILE} 2>&1"
print_done

printf '%b\n' "🙂 All done! You can keep $(basename ${SOLR_PRIVATE_KEY}) and reuse it to sign any other Solr packages."
printf '%b\n' "  Start the SolrCloud cluster again with the following command:"
printf '%b\n\n' "  ${DOCKER_COMPOSE_SOLRCLOUD_COMMAND} up -d"
printf '%b\n\n' "  You can point your browser at http://localhost:8983 to explore your SolrCloud instance."
printf '%b\n' "  Stop the SolrCloud cluster again with the following command:"
printf '%b\n' "  ${DOCKER_COMPOSE_SOLRCLOUD_COMMAND} down"

