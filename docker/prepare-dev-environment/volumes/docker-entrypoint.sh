#!/usr/bin/env bash
lftp -e \
"mirror -vvv /pub/databases/microarray/data/atlas/bioentity_properties /atlas-data/bioentity_properties; exit" \
ftp.ebi.ac.uk

for EXP_ID in ${EXP_IDS}
do
  lftp -e \
  "mirror -vvv /pub/databases/arrayexpress/data/atlas/sc_experiments/${EXP_ID} /atlas-data/scxa/magetab/${EXP_ID}; exit" \
  ftp.ebi.ac.uk
done

lftp -e \
"set xfer:clobber on; \
get /pub/databases/arrayexpress/data/atlas/sc_experiments/cell_stats.json -o /atlas-data/scxa/magetab; exit" \
ftp.ebi.ac.uk
