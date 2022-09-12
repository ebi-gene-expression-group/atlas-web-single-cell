#!/usr/bin/env bash

docker stop scxa-solrcloud-1 scxa-solrcloud-2 scxa-zk-1 scxa-zk-2 scxa-zk-3 scxa-postgres-test scxa-flyway-test scxa-gradle

docker rm scxa-solrcloud-1 scxa-solrcloud-2 scxa-zk-1 scxa-zk-2 scxa-zk-3 scxa-postgres-test scxa-flyway-test scxa-gradle
