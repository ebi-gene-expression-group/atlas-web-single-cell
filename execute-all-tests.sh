#!/usr/bin/env bash

echo "Execute all tests"

export ATLAS_DATA_PATH=${ATLAS_DATA_PATH:-"~/dev/gxa/data"}
POSTGRES_HOST=${POSTGRES_HOST:-"scxa-postgres-test"}
POSTGRES_DB=${POSTGRES_DB:-"gxpscxatest"}
POSTGRES_USER=${POSTGRES_USER:-"scxa"}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-"scxa"}

docker-compose \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
scxa-gradle bash -c "
./gradlew :app:clean &&
./gradlew -PdataFilesLocation=/atlas-data -PexperimentFilesLocation=/atlas-data/scxa -PjdbcUrl=jdbc:postgresql://$POSTGRES_HOST:5432/$POSTGRES_DB -PjdbcUsername=$POSTGRES_USER -PjdbcPassword=$POSTGRES_PASSWORD -PzkHost=scxa-zk-1 -PsolrHost=scxa-solrcloud-1 app:testClasses &&
./gradlew -PtestResultsPath=ut :app:test --tests *Test &&
./gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT &&
./gradlew -PtestResultsPath=e2e :app:test --tests *WIT &&
./gradlew :app:jacocoTestReport
"
