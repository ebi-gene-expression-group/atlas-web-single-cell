#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

SUBPROJECT_NAME=app
DOCKER_COMPOSE_PROJECT_NAME=scxa
ENV_FILE=${SCRIPT_DIR}/docker/dev.env
SCHEMA_VERSION=latest
function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -p SUBPROJECT_NAME ] [ -s SCHEMA_VERSION ] -n TEST_NAME"
  printf '%b\n' "Execute a unit/integration test in a module with the given schema version"
  printf '\n%b\n' "-n\tName of the unit/integration test to execute;\n\tfor example: CellPlotDaoIT"
  printf '\n%b\n' "-p\tName of the sub-project the test can be found;\n\tfor example: app or atlas-web-core (default is ${SUBPROJECT_NAME})"
  printf '\n%b\n' "-s\tNumeric version of the schema or latest\tfor example: 18 (default is ${SCHEMA_VERSION})"
  printf '%b\n\n' "-h\tShow usage instructions"
}

mandatory_name=false
while getopts "n:p:s:h" opt
do
  case ${opt} in
    n )
      mandatory_name=true; TEST_CASE_NAME=${OPTARG}
      ;;
    p )
      SUBPROJECT_NAME=${OPTARG}
      if [[ "$SUBPROJECT_NAME" == "app" ]]; then
        # Default values
        :
      elif [[ "$SUBPROJECT_NAME" == "atlas-web-core" ]]; then
        ENV_FILE=${SCRIPT_DIR}/atlas-web-core/docker/dev.env
        DOCKER_COMPOSE_PROJECT_NAME=gxa
      else
        echo "Project name is not valid: ${OPTARG}" >&2
        exit 1
      fi
      ;;
    s )
      SCHEMA_VERSION=${OPTARG}
      ;;
    h )
      print_usage
      exit 0
      ;;
    \?)
      printf '%b\n' "Invalid option: -${OPTARG}" >&2
      print_usage
      exit 2
      ;;
    : ) echo "Missing option argument for: ${OPTARG}" >&2; exit 1;;
  esac
done

if ! $mandatory_name
then
    echo "-n must be provided with the name of the test to execute" >&2
    print_usage
    exit 1
fi

echo "Testing ${TEST_CASE_NAME}"

source ${ENV_FILE}

SCHEMA_VERSION=${SCHEMA_VERSION} \
docker compose \
--project-name ${DOCKER_COMPOSE_PROJECT_NAME} \
--env-file ${ENV_FILE} \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
gradle bash -c "
set -e

gradle clean

gradle \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/exp \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${DOCKER_COMPOSE_PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${DOCKER_COMPOSE_PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${DOCKER_COMPOSE_PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${DOCKER_COMPOSE_PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${DOCKER_COMPOSE_PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
-PsolrUser=${SOLR_USER} \
-PsolrPassword=${SOLR_PASSWORD} \
--stacktrace \
${SUBPROJECT_NAME}:testClasses

gradle -PsolrUser=${SOLR_USER} -PsolrPassword=${SOLR_PASSWORD} --continuous :${SUBPROJECT_NAME}:test --tests ${TEST_CASE_NAME}
"

SCHEMA_VERSION={SCHEMA_VERSION} \
docker compose \
--project-name ${DOCKER_COMPOSE_PROJECT_NAME} \
--env-file ${ENV_FILE} \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
down
