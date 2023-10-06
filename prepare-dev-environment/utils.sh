function print_stage_name() {
  printf '%b ' "${1}"
  if [ "${LOG_FILE}" = "/dev/stdout" ]; then
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

# Function that displays countdown of 10 seconds preceded by a message. You can skip the countdown by pressing Enter.
# if [ ! -f "/proc/$$/fd/1" ] tests if stdout is not a regular file (e.g. it is a terminal).
function countdown() {
  local count=10
  while [ ${count} -ne 0 ]; do
    if [ ! -f "/proc/$$/fd/1" ]; then
      printf '\r'
    else
      printf '\n'
    fi

    printf '%b in %02d seconds. Press Ctrl+C to cancel or press Enter to continue...' "${1}" "${count}"
    read -r -s -t 1 && break

    count=$((count - 1))
  done
  if [ ! -f "/proc/$$/fd/1" ]; then
    printf '\r\033[2K'
  fi

  print_stage_name "${1}"
}
