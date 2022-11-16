function print_stage_name() {
  printf '%b ' "$1"
  if [ "$LOG_FILE" = "/dev/stdout" ]; then
    printf '\n'
  fi
}

function print_done() {
  printf '%b\n\n' "✅"
}

function print_error() {
  printf '\n\n%b\n' "😢 Something went wrong! See ${LOG_FILE} for more details."
}
trap print_error ERR

# gradle-ro-dep-cache
GRADLE_RO_DEP_CACHE_VOL_NAME=scxa-gradle-ro-dep-cache
GRADLE_RO_DEP_CACHE_DEST=/gradle-ro-dep-cache

# volumes
ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME=scxa-atlas-data-bioentity-properties
ATLAS_DATA_BIOENTITY_PROPERTIES_DEST=/atlas-data/bioentity_properties

ATLAS_DATA_SCXA_VOL_NAME=scxa-atlas-data-scxa
ATLAS_DATA_SCXA_DEST=/atlas-data/scxa

ATLAS_DATA_EXPDESIGN_VOL_NAME=scxa-atlas-data-scxa-expdesign
ATLAS_DATA_EXPDESIGN_DEST=/atlas-data/scxa-expdesign

WEBAPP_PROPERTIES_VOL_NAME=scxa-webapp-properties
WEBAPP_PROPERTIES_DEST=/webapp-properties

EXP_IDS='E-CURD-4 E-EHCA-2 E-GEOD-71585 E-GEOD-81547 E-GEOD-99058 E-MTAB-5061 E-ENAD-53'
