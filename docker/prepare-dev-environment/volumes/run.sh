#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# PROJECT_NAME
# ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME
# ATLAS_DATA_EXP_VOL_NAME
ENV_FILE=${SCRIPT_DIR}/../../dev.env
source ${ENV_FILE}

# print_stage_name
# print_done
# print_error
# countdown
source ${SCRIPT_DIR}/../utils.sh

LOG_FILE=/dev/stdout
REMOVE_VOLUMES=false
function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -r ] [ -l FILE ]"
  printf '%b\n' "Populate two Docker Compose volumes for Single Cell Expression Atlas testing & development."
  printf '%b\n' " • ${PROJECT_NAME}_${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME}:"
  printf '%b\n' "   gene annotations from array designs, Ensembl, Reactome, WormBase ParaSite, miRBase, Gene Ontology"
  printf '%b\n' "   and InterPro"
  printf '%b\n' " • ${PROJECT_NAME}_${ATLAS_DATA_EXP_VOL_NAME}:"
  printf '%b\n' "   test experiments data bundles, species and release definition files"
  printf '\n%b\n' "-r\tRemove volumes before creating them"
  printf '\n%b\n' "-l FILE\tLog file (default is ${LOG_FILE})"
  printf '%b\n\n' "-h\tShow usage instructions"
}


while getopts "l:rh" opt
do
  case ${opt} in
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
--file ${SCRIPT_DIR}/docker-compose.yml"

DOCKER_COMPOSE_COMMAND_VARS="DOCKERFILE_PATH=${SCRIPT_DIR}"

if [ "${REMOVE_VOLUMES}" = "true" ]; then
  countdown "🗑 Remove Docker volumes ${VOL_NAMES[@]}"
  eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --volumes >> ${LOG_FILE} 2>&1"
  print_done
fi

print_stage_name "🛫 Spin up service to download test data"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up --build >> ${LOG_FILE} 2>&1"
print_done

print_stage_name "🛬 Bring down all services"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --rmi local >> ${LOG_FILE} 2>&1"
print_done

printf '%b\n' "🙂 All done! You can inspect the volume contents attaching them to a container:"
printf '%b\n' "   docker run \\"
printf '%b\n' "   -v ${PROJECT_NAME}_${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME}:/${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME} \\"
printf '%b\n' "   -v ${PROJECT_NAME}_${ATLAS_DATA_EXP_VOL_NAME}:/${ATLAS_DATA_EXP_VOL_NAME} \\"
printf '%b\n' "   --rm -it ubuntu:jammy bash"
