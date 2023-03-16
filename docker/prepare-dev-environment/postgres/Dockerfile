FROM ubuntu:jammy

# Remove references to broken/outdated packages; otherwise we can get 404s:
# E: Failed to fetch http://archive.ubuntu.com/ubuntu/pool/main/r/rsync/rsync_3.2.3-8ubuntu3.1_amd64.deb  404  Not Found [IP: 185.125.190.39 80
RUN rm -rf /var/lib/apt/lists/*
RUN apt -y -qq update
RUN apt -y -qq install wget gnupg lsb-core
# https://www.postgresql.org/download/linux/ubuntu/
RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
RUN wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc > /etc/apt/trusted.gpg.d/apt.postgresql.org.asc
RUN apt -y -qq update
RUN DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt -y -qq install openjdk-11-jdk git sed gawk jq r-base r-cran-optparse r-cran-tidyr r-cran-data.table nodejs postgresql-client-11

VOLUME /atlas-data/scxa /atlas-data/scxa-expdesign

WORKDIR /root
RUN git clone --recurse-submodules --depth 1 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git
RUN git clone --recurse-submodules https://github.com/ebi-gene-expression-group/db-scxa.git

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
