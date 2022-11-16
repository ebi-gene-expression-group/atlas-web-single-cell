#!/usr/bin/env bash
set -e

echo "Execute all tests"

export POSTGRES_HOST=${POSTGRES_HOST:-"scxa-postgres-test"}
export POSTGRES_DB=${POSTGRES_DB:-"gxpscxatest"}
export POSTGRES_USER=${POSTGRES_USER:-"scxa"}
export POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-"scxa"}

docker-compose \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
scxa-gradle bash -c "
set -e

./gradlew clean

./gradlew \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://$POSTGRES_HOST:5432/$POSTGRES_DB \
-PjdbcUsername=$POSTGRES_USER \
-PjdbcPassword=$POSTGRES_PASSWORD \
-PzkHosts=scxa-solrcloud-zookeeper-0:2181,scxa-solrcloud-zookeeper-1:2181,scxa-solrcloud-zookeeper-2:2181 \
-PsolrHosts=http://scxa-solrcloud-0:8983/solr,http://scxa-solrcloud-1:8983/solr \
atlas-web-core:testClasses

./gradlew -PtestResultsPath=ut :atlas-web-core:test --tests *Test
#sh./gradlew -PtestResultsPath=it :atlas-web-core:test --tests *IT
./gradlew :atlas-web-core:jacocoTestReport

./gradlew \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://$POSTGRES_HOST:5432/$POSTGRES_DB \
-PjdbcUsername=$POSTGRES_USER \
-PjdbcPassword=$POSTGRES_PASSWORD \
-PzkHosts=scxa-solrcloud-zookeeper-0:2181,scxa-solrcloud-zookeeper-1:2181,scxa-solrcloud-zookeeper-2:2181 \
-PsolrHosts=http://scxa-solrcloud-0:8983/solr,http://scxa-solrcloud-1:8983/solr \
app:testClasses

./gradlew -PtestResultsPath=ut :app:test --tests *Test
./gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT
./gradlew -PtestResultsPath=e2e :app:test --tests *WIT
./gradlew :app:jacocoTestReport
"
