#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source ${SCRIPT_DIR}/docker/dev.env

function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -p SUBPROJECT_NAME ] [ -s SCHEMA_VERSION ] -n TEST_NAME"
  printf '%b\n' "Execute a unit/integration test in a module with the given schema version"
  printf '\n%b\n' "-n\tName of the unit/integration test to execute;\n\tfor example: CellPlotDaoIT"
  printf '\n%b\n' "-p\tName of the sub-project the test can be found;\n\tfor example: app or atlas-web-core (default is app)"
  printf '\n%b\n' "-s\tNumeric version of the schema or latest\tfor example: 18 (default is latest)"
  printf '%b\n\n' "-h\tShow usage instructions"
}

PROJECT_NAME=app
SCHEMA_VERSION=latest
mandatory_name=false

while getopts "n:p:s:h" opt
do
  case ${opt} in
    n )
      mandatory_name=true; TEST_CASE_NAME=${OPTARG}
      ;;
    p )
      PROJECT_NAME=${OPTARG}
      if ! [[ "$PROJECT_NAME" =~ ^(app|atlas-web-core)$ ]]; then
        echo "Project name is not valid -$OPTARG" >&2
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
    : ) echo "Missing option argument for: $OPTARG" >&2; exit 1;;
  esac
done

if ! $mandatory_name
then
    echo "-n must be provided with the name of the test to execute" >&2
    exit 1
fi

echo "Testing ${TEST_CASE_NAME}"

SCHEMA_VERSION={SCHEMA_VERSION} \
docker-compose \
--env-file ${SCRIPT_DIR}/docker/dev.env \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
scxa-gradle bash -c "
set -e
gradle clean

gradle \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
--stacktrace \
${PROJECT_NAME}:testClasses

gradle --continuous :${PROJECT_NAME}:test --tests $TEST_CASE_NAME
"
