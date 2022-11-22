#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# EXP_IDS
source ${SCRIPT_DIR}/../test-data.env
# ATLAS_DATA_SCXA_EXPDESIGN_VOL_NAME
# GRADLE_RO_DEP_CACHE_VOL_NAME
# POSTGRES_HOST
# POSTGRES_DB
# POSTGRES_USER
# POSTGRES_PASSWORD
source ${SCRIPT_DIR}/../../dev.env
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n' "Usage: ${0} [ -v NUMBER ] [ -l FILE ]"
  printf '\n%b\n' "Populate a Single Cell Expression Atlas Postgres 11 database."
  printf '\n%b\n' "-a\tdisable anndata support (i.e. migrates database to v18)"
  printf '\n%b\n' "-l FILE\tLog file (default is /dev/stdout)"
  printf '%b\n\n' "-h\tDisplay usage instructions"
}

LOG_FILE=/dev/stdout
SCHEMA_VERSION=latest
while getopts "al:h" opt
do
  case ${opt} in
    a)
      SCHEMA_VERSION=18
      ;;
    l)
      LOG_FILE=${OPTARG}
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

IMAGE_NAME=scxa-postgres-loader
print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build \
-t ${IMAGE_NAME} ${SCRIPT_DIR} &>> ${LOG_FILE}
print_done

print_stage_name "🐘 Start Postgres 11 in Docker Compose"
SCHEMA_VERSION=${SCHEMA_VERSION} \
docker-compose \
--env-file ${SCRIPT_DIR}/../../dev.env \
-f ${SCRIPT_DIR}/../../docker-compose-postgres.yml \
up -d &>> ${LOG_FILE}
print_done

print_stage_name "💤 Wait for twenty seconds to apply migrations and Postgres server be ready to work"
sleep 20
print_done

print_stage_name "⚙ Spin up containers to index volume data in Postgres"
GRADLE_RO_DEP_CACHE_DEST=/gradle-ro-dep-cache
# Test data volume needs to be mounted in RW mode because db-scxa scripts write temp files in the magetab directory
docker run --rm \
-v ${ATLAS_DATA_SCXA_VOL_NAME}:/atlas-data/scxa:rw \
-v ${ATLAS_DATA_SCXA_EXPDESIGN_VOL_NAME}:/atlas-data/scxa-expdesign:rw \
-v ${GRADLE_RO_DEP_CACHE_VOL_NAME}:${GRADLE_RO_DEP_CACHE_DEST}:ro \
-e POSTGRES_HOST=${POSTGRES_HOST} \
-e POSTGRES_DB=${POSTGRES_DB} \
-e POSTGRES_USER=${POSTGRES_USER} \
-e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
-e EXP_IDS="${EXP_IDS}" \
-e SCHEMA_VERSION=${SCHEMA_VERSION} \
-e GRADLE_RO_DEP_CACHE=${GRADLE_RO_DEP_CACHE_DEST} \
--network atlas-test-net \
${IMAGE_NAME} &>> ${LOG_FILE}
print_done

printf '%b\n' "🙂 All done! Remember to set the environment variable SCHEMA_VERSION=${SCHEMA_VERSION} when creating/starting the Docker Compose Postgres service."
printf '%b\n' "   The Postgres 11 container exposes port 5432, so you should be able to connect with psql:"
printf '%b\n' "   psql -h localhost -d ${POSTGRES_DB} -U ${POSTGRES_USER} # The password is ${POSTGRES_PASSWORD}"
