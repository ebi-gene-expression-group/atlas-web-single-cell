#!/usr/bin/env bash
docker build --no-cache -t scxa-volumes-populator ./scxa-volumes-populator
docker build --no-cache -t scxa-solrcloud-8-indexer ./scxa-solrcloud-8-indexer

docker volume create bioentity-properties
docker volume create scxa-data
docker run --rm -v bioentity-properties:/bioentity_properties -v scxa-data:/scxa-data scxa-volumes-populator

# To test it...
# docker run -it --rm -v bioentity-properties:/bioentity_properties -v scxa-data:/scxa-data busybox:latest

# You MUST set:
#   SC_ATLAS_OWL -> /path/to/scatlas.owl
#   SOLR_KEYS_PATH -> /path/to/scxa-solrcloud.pem|scxa-solrcloud.der [YOU NEED BOTH FILES]
docker-compose -f ../docker-compose-solrcloud-8.yml up -d
sleep 10s
docker cp ${SC_ATLAS_OWL} solr-8.7-scxa-solrcloud-0:/var/solr/data/userfiles/
docker cp ${SC_ATLAS_OWL} solr-8.7-scxa-solrcloud-1:/var/solr/data/userfiles/
docker exec solr-8.7-scxa-solrcloud-0 ./bin/solr package add-key /run/secrets/scxa-solrcloud.der

docker run --network atlas-test-net --name dummy-postgres --env POSTGRES_PASSWORD=foobar -d postgres:10-alpine
# Docker secrets only work in Swarm mode so we use a mounted volume with the keys
docker run --network atlas-test-net --rm -v ${SOLR_KEYS_PATH}:/root/secrets:ro -v bioentity-properties:/bioentity_properties -v scxa-data:/scxa-data scxa-solrcloud-8-indexer
docker stop dummy-postgres && docker rm dummy-postgres
