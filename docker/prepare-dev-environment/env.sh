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

GRADLE_RO_DEP_CACHE_VOL_NAME=scxa-gradle-ro-dep-cache
GRADLE_RO_DEP_CACHE_DEST=/gradle-ro-dep-cache
