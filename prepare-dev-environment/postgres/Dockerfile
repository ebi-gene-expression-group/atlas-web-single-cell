FROM ubuntu:jammy

# Update package list and install necessary tools
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    wget gnupg lsb-release ca-certificates

# Add PostgreSQL repository
RUN echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list && \
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -

# Update package list again and install required dependencies
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt-get install -y --no-install-recommends \
    openjdk-11-jdk git sed gawk jq r-base r-cran-optparse r-cran-tidyr r-cran-data.table nodejs postgresql-client-11

# Clean up APT cache and temporary files
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Set working directory and clone necessary repositories
WORKDIR /root
RUN git clone --recurse-submodules --depth 1 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git && \
    git clone --recurse-submodules https://github.com/ebi-gene-expression-group/db-scxa.git

ENTRYPOINT ["/bin/bash", "-c"]