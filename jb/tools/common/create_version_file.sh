#!/bin/bash
if [ "${OS}" != "windows" ]; then
    set -euo pipefail
fi

echo "*** create jcef.version..."
grep "#define JCEF_VERSION" "$JCEF_ROOT_DIR"/native/jcef_version.h | sed 's/#define JCEF_VERSION /JCEF_VERSION=/g' > "$1/jcef.version"
grep "#define JCEF_COMMIT_HASH" "$JCEF_ROOT_DIR"/native/jcef_version.h | sed 's/#define JCEF_COMMIT_HASH /JCEF_COMMIT_HASH=/g' | tee -a "$1/jcef.version"
{
  printf "JCEF_VERSION_DETAILED="
  cat "$OUT_CLS_DIR"/com/jetbrains/cef/version.info
} >> "$1/jcef.version"
echo >> "$1/jcef.version" # newline
echo "jcef.version:"
cat "$1/jcef.version"
