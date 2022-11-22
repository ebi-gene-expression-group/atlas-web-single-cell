FROM ubuntu:jammy

RUN apt -y -qq update
# Beware! Python 3 is required by index-bioentities but comes as part of Ubuntu
RUN apt -y -qq install git openjdk-11-jdk jq rsync curl

RUN mkdir -p /root/bioentity-properties-jsonl

WORKDIR /root
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-bioentities.git
RUN git clone --depth 1 --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-bulk.git
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-scxa.git

VOLUME /atlas-data/bioentity_properties /atlas-data/scxa

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
