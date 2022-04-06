#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

JCEF_ROOT_DIR="$(cd "$script_dir/../../.." && pwd)"
export JCEF_ROOT_DIR
echo "JCEF_ROOT_DIR is $JCEF_ROOT_DIR"
export JB_TOOLS_DIR="$JCEF_ROOT_DIR"/jb/tools
export JB_TOOLS_OS_DIR="$JB_TOOLS_DIR"/mac
export MODULAR_SDK_DIR="$JCEF_ROOT_DIR"/jcef_build/native/Release/modular-sdk
export OS=macosx
export DEPS_ARCH=universal

if [ -z "${JDK_11:-}" ]; then
    echo "error: JDK_11 is not set"
    exit 1
fi
export JAVA_HOME="$JDK_11"
echo "JAVA_HOME is $JAVA_HOME"

# shellcheck disable=SC2230
if ! which ant &>/dev/null; then
  if [ -z "${ANT_HOME:-}" ]; then
    echo "error: ANT_HOME is not set nor ant present in PATH"
    exit 1
  fi
  export PATH="$ANT_HOME/bin:$PATH"
  echo "ANT_HOME is $ANT_HOME"
fi
