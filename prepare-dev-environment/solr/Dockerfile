FROM ubuntu:jammy

# Update package list and install necessary tools
# Python 3 is required by index-bioentities but comes as part of Ubuntu
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    git openjdk-11-jdk jq rsync curl

# Clean up APT cache and temporary files
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Create the necessary directory
RUN mkdir -p /root/bioentity-properties-jsonl

# Clone the necessary repositories
WORKDIR /root
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-bioentities.git
RUN git clone --depth 1 --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-bulk.git
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-scxa.git

ENTRYPOINT ["/bin/bash", "-c"]
