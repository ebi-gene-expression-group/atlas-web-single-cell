# SolrCloud 8 cluster bootstrapping

Choose a path and generate keys to sign and verify Solr packages; get latest ontology OWL file and run the script:
```bash
export SOLR_KEYS_PATH=/some/path/you/like
openssl genrsa -out ${SOLR_KEYS_PATH}/scxa-solrcloud.pem 512
openssl rsa -in ${SOLR_KEYS_PATH}/scxa-solrcloud.pem -pubout -outform DER -out ${SOLR_KEYS_PATH}/scxa-solrcloud.der

export SC_ATLAS_OWL=/path/to/cloned/repo/of/scatlas_ontology/scatlas.owl

./run.sh
```

The script will create two volumes, `bioentity-properties` and `scxa-data`, and run an epehemeral container to populate
them ; it will then bring up a SolrCloud cluster via `docker-compose` and another epehemeral container will send the
data in the volumes to be indexed in Solr.

At the end of the process you can visit `localhost:8983` and look at a fully working Solr instance.