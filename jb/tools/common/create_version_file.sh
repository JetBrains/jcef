#!/bin/bash
set -euo pipefail

echo "*** create jcef.version..."
grep "#define JCEF_VERSION" "$JCEF_ROOT_DIR"/native/jcef_version.h | sed 's/#define JCEF_VERSION /JCEF_VERSION=/g' > "$1/jcef.version"
