#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# GRADLE_RO_DEP_CACHE_VOL_NAME
source ${SCRIPT_DIR}/../../dev.env
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -l FILE ]"
  printf '%b\n' "Create and populate a Gradle RO dependency cache volume to speed up builds of Single Cell Expression Atlas."
  printf '\n%b\n\n' "-h\tShow usage instructions"
}

LOG_FILE=/dev/stdout
while getopts "l:h" opt
do
  case $opt in
    l)
      LOG_FILE=$OPTARG
      ;;
    h)
      print_usage
      exit 0
      ;;
    \?)
      printf '%b\n' "Invalid option: -$OPTARG" >&2
      print_usage
      exit 2
      ;;
  esac
done

print_stage_name "🗑 Remove previous version of ${GRADLE_RO_DEP_CACHE_VOL_NAME} if it exists"
docker volume rm ${GRADLE_RO_DEP_CACHE_VOL_NAME} &>> ${LOG_FILE} || true
print_done

print_stage_name "💾 Create Docker volume ${GRADLE_RO_DEP_CACHE_VOL_NAME}"
docker volume create ${GRADLE_RO_DEP_CACHE_VOL_NAME} &>> ${LOG_FILE}
print_done

IMAGE_NAME=scxa-gradle-ro-dep-cache-builder
print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build \
--build-arg GRADLE_RO_DEP_CACHE_DEST=${GRADLE_RO_DEP_CACHE_DEST} \
-t ${IMAGE_NAME} ${SCRIPT_DIR} &>> ${LOG_FILE}
print_done

print_stage_name "⚙ Spin up ephemeral container to copy local artifacts to dependency cache volume"
docker run --rm \
-v ${GRADLE_RO_DEP_CACHE_VOL_NAME}:${GRADLE_RO_DEP_CACHE_DEST} \
${IMAGE_NAME} &>> ${LOG_FILE}
print_done

printf '%b\n' "🙂 All done! You can inspect the volume contents mounting it in a container:"
printf '%b\n' "   docker run --rm \\"
printf '%b\n' "   -v ${GRADLE_RO_DEP_CACHE_VOL_NAME}:${GRADLE_RO_DEP_CACHE_DEST} \\"
printf '%b\n' "   -it ubuntu:jammy bash"
