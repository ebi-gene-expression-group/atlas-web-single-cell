#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# POSTGRES_HOST
# POSTGRES_DB
# POSTGRES_USER
# POSTGRES_PASSWORD
# ATLAS_DATA
# ATLAS_DATA_SCXA_DEST
# ATLAS_DATA_SCXA_EXPDESIGN_VOL_NAME
# ATLAS_DATA_SCXA_EXPDESIGN_DEST
# GRADLE_RO_DEP_CACHE_VOL_NAME
# GRADLE_RO_DEP_CACHE_DEST
# EXP_IDS
# print_stage_name
# print_done
source ${SCRIPT_DIR}/../scxa.env
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n' "Usage: $0 [ -v NUMBER ] [ -l FILE ]"
  printf '\n%b\n' "Populate a Single Cell Expression Atlas Postgres 11 database."
  printf '\n%b\n' "-a\tdisable anndata support (i.e. migrates database to v18)"
  printf '\n%b\n' "-l FILE\tLog file (default is /dev/stdout)"
  printf '%b\n\n' "-h\tDisplay usage instructions"
}

LOG_FILE=/dev/stdout
SCHEMA_VERSION=latest
while getopts "al:h" opt
do
  case $opt in
    a)
      SCHEMA_VERSION=18
      ;;
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

IMAGE_NAME=scxa-postgres-loader
print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build \
--build-arg POSTGRES_HOST=${POSTGRES_HOST} \
--build-arg POSTGRES_DB=${POSTGRES_DB} \
--build-arg POSTGRES_USER=${POSTGRES_USER} \
--build-arg POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
--build-arg SCHEMA_VERSION=${SCHEMA_VERSION} \
--build-arg EXP_IDS="${EXP_IDS}" \
--build-arg ATLAS_DATA=${ATLAS_DATA} \
--build-arg ATLAS_DATA_SCXA_DEST=${ATLAS_DATA_SCXA_DEST} \
--build-arg GRADLE_RO_DEP_CACHE_DEST=${GRADLE_RO_DEP_CACHE_DEST} \
-t ${IMAGE_NAME} ${SCRIPT_DIR} &>> ${LOG_FILE}
print_done

print_stage_name "🐘 Start Postgres 11 in Docker Compose"
SCHEMA_VERSION=${SCHEMA_VERSION} \
docker-compose -f ${SCRIPT_DIR}/../../docker-compose-postgres.yml up -d &>> ${LOG_FILE}
print_done

print_stage_name "💤 Wait for twenty seconds to apply migrations and Postgres server be ready to work"
sleep 20
print_done

print_stage_name "⚙ Spin up containers to index volume data in Postgres"
SOLR_CONTAINER_ID=$(docker run --rm -d -h solr-foo --network atlas-test-net solr:8.7.0 solr start -c -f)

# Test data volume needs to be mounted in RW mode because db-scxa scripts write temp files in the magetab directory
docker run --rm \
-v ${ATLAS_DATA_SCXA_VOL_NAME}:${ATLAS_DATA_SCXA_DEST}:rw \
-v ${ATLAS_DATA_SCXA_EXPDESIGN_VOL_NAME}:${ATLAS_DATA_SCXA_EXPDESIGN_DEST}:rw \
-v ${GRADLE_RO_DEP_CACHE_VOL_NAME}:${GRADLE_RO_DEP_CACHE_DEST}:ro \
--network atlas-test-net \
${IMAGE_NAME} &>> ${LOG_FILE}
print_done

print_stage_name "🧹 Clean up additional containers"
docker stop ${SOLR_CONTAINER_ID}
print_done

printf '%b\n' "🙂 All done! Remember to set the environment variable SCHEMA_VERSION=${SCHEMA_VERSION} when creating/starting the Docker Compose Postgres service."
printf '%b\n' "   The Postgres 11 container exposes port 5432, so you should be able to connect with psql:"
printf '%b\n' "   psql -h localhost -d ${POSTGRES_DB} -U ${POSTGRES_USER} # The password is ${POSTGRES_PASSWORD}"
