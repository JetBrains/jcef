#!/bin/bash
if [ "${OS}" != "windows" ]; then
    set -euo pipefail
fi

echo "*** create jcef.version..."
grep "#define JCEF_VERSION" "$JCEF_ROOT_DIR"/native/jcef_version.h | sed 's/#define JCEF_VERSION /JCEF_VERSION=/g' > "$1/jcef.version"
grep "#define JCEF_COMMIT_HASH" "$JCEF_ROOT_DIR"/native/jcef_version.h | sed 's/#define JCEF_COMMIT_HASH /JCEF_COMMIT_HASH=/g' | tee -a "$1/jcef.version"
