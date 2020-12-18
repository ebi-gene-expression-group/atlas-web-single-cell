FROM alpine:3.12

RUN apk --no-cache add bash curl git grep

RUN git clone https://github.com/ebi-gene-expression-group/index-scxa.git \
    && git clone https://github.com/ebi-gene-expression-group/index-gxa.git

COPY scxa-solrcloud-bootstrap-entrypoint.sh /entrypoint.sh

# Collection .system is populated with BioSolr’s JAR blob when creating the scxa-analytics collection
ENV SOLR_COLLECTIONS="scxa-analytics-v3 scxa-analytics-v4 scxa-analytics-v5 scxa-gene2experiment-v1 bioentities-v1"
ENV SOLR_HOSTS="scxa-solrcloud-1:8983 scxa-solrcloud-2:8983"
ENV SOLR_HOST="scxa-solrcloud-1:8983"
ENV SOLR_NUM_SHARDS=2

ENTRYPOINT ["/entrypoint.sh"]
