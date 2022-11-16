#!/usr/bin/env bash
lftp -e "mirror -vvv /pub/databases/microarray/data/atlas/bioentity_properties ${ATLAS_DATA_BIOENTITY_PROPERTIES_DEST}; exit" ftp.ebi.ac.uk
for EXP_ID in ${EXP_IDS}; do lftp -e "mirror -vvv /pub/databases/arrayexpress/data/atlas/sc_experiments/${EXP_ID} ${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID}; exit" ftp.ebi.ac.uk; done
lftp -e "set xfer:clobber on; get /pub/databases/arrayexpress/data/atlas/sc_experiments/cell_stats.json -o ${ATLAS_DATA_SCXA_DEST}/magetab; exit" ftp.ebi.ac.uk
