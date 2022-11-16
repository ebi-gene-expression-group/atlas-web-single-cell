FROM ubuntu:jammy

ARG GRADLE_RO_DEP_CACHE_DEST
ENV GRADLE_RO_DEP_CACHE_DEST=${GRADLE_RO_DEP_CACHE_DEST}

RUN apt -y -qq update
RUN apt install -y -qq git openjdk-11-jdk rsync

WORKDIR /root
RUN mkdir -p ${GRADLE_RO_DEP_CACHE_DEST}
RUN git clone --recurse-submodules --depth 1 --branch chore/solr8 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git

ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8
WORKDIR /root/atlas-web-single-cell

VOLUME ${GRADLE_RO_DEP_CACHE_DEST}

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
