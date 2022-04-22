#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

source "$script_dir/set_env.sh"

OUT_DIR="$JCEF_ROOT_DIR/out"

if [ "${1:-}" == "clean" ]; then
  echo "*** delete $OUT_DIR..."
  rm -rf "$OUT_DIR"
  exit 0
fi
rm -rf "$OUT_DIR" && mkdir "$OUT_DIR"

echo "*** compile java sources..."
bash "$JCEF_ROOT_DIR"/tools/compile.sh linux64 Release
