#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

usage() {
  echo "Usage: $(basename "$0") <path>" >&2
  echo "Env:   CODESIGN_CLIENT must be set to the signing client executable" >&2
}

if [[ $# -ne 1 ]]; then
  usage
  exit 2
fi

if [[ -z "${CODESIGN_CLIENT:-}" ]]; then
  echo "Error: CODESIGN_CLIENT env var is not set." >&2
  exit 2
fi

INPUT_PATH="$1"

if [[ ! -e "$INPUT_PATH" ]]; then
  echo "Error: path does not exist: $INPUT_PATH" >&2
  exit 1
fi

if [[ ! -x "$CODESIGN_CLIENT" ]]; then
  echo "Error: CODESIGN_CLIENT is not executable: $CODESIGN_CLIENT" >&2
  exit 1
fi

SIGNED_DIR="./signed"
TMP_DIR="./code_sign_tmp"
TMP_TAR="$TMP_DIR/to_be_signed.tar.gz"

cleanup() {
  rm -rf "$TMP_DIR" "$SIGNED_DIR"
}
trap cleanup EXIT

mkdir -p "$TMP_DIR"

INPUT_ABS="$(cd "$(dirname "$INPUT_PATH")" && pwd)/$(basename "$INPUT_PATH")"
INPUT_DIRNAME="$(dirname "$INPUT_ABS")"
INPUT_BASENAME="$(basename "$INPUT_ABS")"

EXEC_ABS_PATHS=()

snapshot_execs() {
    EXEC_ABS_PATHS=()
    while IFS= read -r -d '' f; do
      EXEC_ABS_PATHS+=("$f")
      echo "Found executable: '$f'" >&2
    done < <(find "$INPUT_ABS" -type f -perm -111 -print0)
}

restore_exec() {
  local f
  for f in "${EXEC_ABS_PATHS[@]}"; do
    if [[ -f "$f" ]]; then
      chmod a+x "$f"
      echo "Restored executable: '$f'" >&2
    fi
  done
}

sign_with_retry() {
  local attempts=3
  local delay_sec=30
  local i

  for ((i=1; i<=attempts; i++)); do
    rm -rf "$SIGNED_DIR"
    mkdir -p "$SIGNED_DIR"

    echo "Signing attempt $i/$attempts: $*" >&2
    if "$CODESIGN_CLIENT" "$@"; then
      return 0
    fi

    if (( i < attempts )); then
      echo "Signing failed (attempt $i/$attempts). Sleeping ${delay_sec}s before retry..." >&2
      sleep "$delay_sec"
    fi
  done

  echo "Error: signing failed after $attempts attempts." >&2
  return 1
}

# Begin

snapshot_execs

if [[ -f "$INPUT_ABS" ]]; then
  sign_with_retry -log-format text -max-wait 30m -denoted-content-type application/x-mac-app-bin "$INPUT_ABS"

  SIGNED_FILE="$SIGNED_DIR/$INPUT_BASENAME"
  if [[ ! -f "$SIGNED_FILE" ]]; then
    echo "Error: expected signed file not found: $SIGNED_FILE" >&2
    exit 1
  fi

  rm -f "$INPUT_ABS"
  mv -f "$SIGNED_FILE" "$INPUT_DIRNAME/$INPUT_BASENAME"

elif [[ -d "$INPUT_ABS" ]]; then
  rm -f "$TMP_TAR"
  tar -pczf "$TMP_TAR" -C "$INPUT_DIRNAME" "$INPUT_BASENAME"

  sign_with_retry -log-format text -max-wait 30m -denoted-content-type application/x-mac-app-targz -extensions mac_codesign_identity="$CODESIGN_STRING",mac_codesign_options=runtime,mac_codesign_force=true,mac_codesign_entitlements="$script_dir"/entitlements.xml "$TMP_TAR"

  SIGNED_TAR="$SIGNED_DIR/$(basename "$TMP_TAR")"
  if [[ ! -f "$SIGNED_TAR" ]]; then
    echo "Error: expected signed archive not found: $SIGNED_TAR" >&2
    exit 1
  fi

  rm -rf "$INPUT_ABS"

  tar -xzf "$SIGNED_TAR" -C "$INPUT_DIRNAME"
  rm -f "$SIGNED_TAR"

else
  echo "Error: path is neither a regular file nor a directory: $INPUT_ABS" >&2
  exit 1
fi

restore_exec

# End
