#!/usr/bin/env bash
docker stop \
scxa-solrcloud-0 scxa-solrcloud-1 \
scxa-solrcloud-zookeeper-0 scxa-solrcloud-zookeeper-1 scxa-solrcloud-zookeeper-2 \
scxa-postgres-test scxa-flyway-test \
scxa-gradle

docker rm \
scxa-solrcloud-0 scxa-solrcloud-1 \
scxa-solrcloud-zookeeper-0 scxa-solrcloud-zookeeper-1 scxa-solrcloud-zookeeper-2 \
scxa-postgres-test scxa-flyway-test \
scxa-gradle
