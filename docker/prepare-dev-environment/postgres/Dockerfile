FROM ubuntu:jammy

RUN apt -y -qq update
RUN apt install -y wget gnupg lsb-core
# https://www.postgresql.org/download/linux/ubuntu/
RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
RUN wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc > /etc/apt/trusted.gpg.d/apt.postgresql.org.asc

RUN apt -y -qq update
RUN DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt install -y -qq openjdk-11-jdk git sed gawk jq postgresql-client-11 r-base r-cran-optparse r-cran-tidyr r-cran-data.table nodejs

WORKDIR /root
RUN git clone --recurse-submodules --depth 1 --branch chore/solr8 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git
RUN git clone --recurse-submodules https://github.com/ebi-gene-expression-group/db-scxa.git

ARG POSTGRES_HOST
ENV POSTGRES_HOST=${POSTGRES_HOST}
ARG POSTGRES_DB
ENV POSTGRES_DB=${POSTGRES_DB}
ARG POSTGRES_USER
ENV POSTGRES_USER=${POSTGRES_USER}
ARG POSTGRES_PASSWORD
ENV POSTGRES_PASSWORD=${POSTGRES_PASSWORD}

ENV dbConnection postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@${POSTGRES_HOST}:5432/${POSTGRES_DB}

ARG SCHEMA_VERSION
ENV SCHEMA_VERSION=${SCHEMA_VERSION}

ARG EXP_IDS
ENV EXP_IDS=${EXP_IDS}

ARG ATLAS_DATA
ENV ATLAS_DATA=${ATLAS_DATA}
ARG ATLAS_DATA_SCXA_DEST
ENV ATLAS_DATA_SCXA_DEST=${ATLAS_DATA_SCXA_DEST}

ENV EXPRESSION_TYPE aggregated_filtered_normalised_counts
ENV CLUSTERS_FORMAT SCANPY
ARG EXP_IDS
ENV EXP_IDS=${EXP_IDS}

ARG GRADLE_RO_DEP_CACHE_DEST
ENV GRADLE_RO_DEP_CACHE=${GRADLE_RO_DEP_CACHE_DEST}
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
