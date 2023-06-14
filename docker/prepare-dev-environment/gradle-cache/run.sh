#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

#PROJECT_NAME=scxa
#GRADLE_WRAPPER_DISTS_VOL_NAME=gradle-wrapper-dists
#GRADLE_RO_DEP_CACHE_VOL_NAME=gradle-ro-dep-cache
ENV_FILE=${SCRIPT_DIR}/../../dev.env
source ${ENV_FILE}

# countdown
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

REMOVE_VOLUMES=false
LOG_FILE=/dev/stdout
function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [-r] [ -l FILE ]"
  printf '%b\n' "Populate Gradle wrapper and RO dependency cache Docker Compose volumes to speed up builds of"
  printf '%b\n' "Single Cell Expression Atlas."
  printf '\n%b\n' "-r\tRemove volumes before creating them"
  printf '\n%b\n' "-l FILE\tLog file (default is ${LOG_FILE})"
  printf '%b\n\n' "-h\tShow usage instructions"
}

while getopts "l:rh" opt
do
  case ${opt} in
    l)
      LOG_FILE=${OPTARG}
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
--file ${SCRIPT_DIR}/docker-compose.yml"

DOCKER_COMPOSE_COMMAND_VARS="DOCKERFILE_PATH=${SCRIPT_DIR}"

if [ "${REMOVE_VOLUMES}" = "true" ]; then
  countdown "🗑 Remove previous version of Gradle cache volumes"
  eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --volumes >> ${LOG_FILE} 2>&1"
  print_done
fi

print_stage_name "🛫 Spin up service to copy local artifacts to dependency cache volume"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up --build >> ${LOG_FILE} 2>&1"
print_done

print_stage_name "🛬 Bring down all services"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --rmi local >> ${LOG_FILE} 2>&1"
print_done

printf '%b\n' "🙂 All done! You can inspect the volume contents mounting it in a container:"
printf '%b\n' "   docker run --rm \\"
printf '%b\n' "   -v ${PROJECT_NAME}_${GRADLE_WRAPPER_DISTS_VOL_NAME}:/${GRADLE_WRAPPER_DISTS_VOL_NAME} \\"
printf '%b\n' "   -v ${PROJECT_NAME}_${GRADLE_RO_DEP_CACHE_VOL_NAME}:/${GRADLE_RO_DEP_CACHE_VOL_NAME} \\"
printf '%b\n' "   -it ubuntu:jammy bash"
