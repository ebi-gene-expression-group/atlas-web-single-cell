#!/usr/bin/env bash
lftp -e "mirror -vvv /pub/databases/microarray/data/atlas/bioentity_properties /bioentity_properties; exit" ftp.ebi.ac.uk
for EXP_ID in ${EXP_IDS}; do lftp -e "mirror -vvv /pub/databases/arrayexpress/data/atlas/sc_experiments/${EXP_ID} /scxa-data/magetab/${EXP_ID}; exit" ftp.ebi.ac.uk; done
lftp -e "set xfer:clobber on; get /pub/databases/arrayexpress/data/atlas/sc_experiments/cell_stats.json -o /scxa-data/magetab; exit" ftp.ebi.ac.uk
