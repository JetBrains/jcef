#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

export JOGAMP_DIR="$JCEF_ROOT_DIR/third_party/jogamp/jar"

function extract_jar {
  __jar=$1
  __dst_dir=$2
  __content_dir="${3:-.}"

  rm -rf __tmp
  mkdir __tmp
  cd __tmp || exit 1

  "$JAVA_HOME"/bin/jar -xf "$__jar"
  rm -rf META-INF

  mkdir -p "$__dst_dir"
  cp -R "$__content_dir"/* "$__dst_dir"

  cd .. || exit 1
  rm -rf __tmp
}

cd "$JCEF_ROOT_DIR" || exit 1

echo "*** bundle jogl and gluegen modules..."
extract_jar "$JOGAMP_DIR"/jogl-all.jar "$MODULAR_SDK_DIR"/modules/jogl.all
extract_jar "$JOGAMP_DIR"/gluegen-rt.jar "$MODULAR_SDK_DIR"/modules/gluegen.rt

echo "*** bundle jogl and gluegen modules_libs..."
extract_jar "$JOGAMP_DIR/jogl-all-natives-${OS}-${DEPS_ARCH}.jar" "$MODULAR_SDK_DIR"/modules_libs/jogl.all "natives/${OS}-${DEPS_ARCH}"
extract_jar "$JOGAMP_DIR/gluegen-rt-natives-${OS}-${DEPS_ARCH}.jar" "$MODULAR_SDK_DIR"/modules_libs/gluegen.rt "natives/${OS}-${DEPS_ARCH}"

echo "*** bundle jogl and gluegen modules_src..."
mkdir -p "$MODULAR_SDK_DIR"/modules_src/jogl.all
cp "${JB_TOOLS_OS_DIR}"/jogl-module-info-java.txt "$MODULAR_SDK_DIR"/modules_src/jogl.all/module-info.java

mkdir -p "$MODULAR_SDK_DIR"/modules_src/gluegen.rt
cp "$JB_TOOLS_DIR"/common/gluegen-module-info-java.txt "$MODULAR_SDK_DIR"/modules_src/gluegen.rt/module-info.java

echo "*** bundle jogl and gluegen make..."
mkdir -p "$MODULAR_SDK_DIR"/make/jogl.all
cp "$JB_TOOLS_DIR"/common/modular-sdk-build-properties.txt "$MODULAR_SDK_DIR"/make/jogl.all/build.properties

mkdir -p "$MODULAR_SDK_DIR"/make/gluegen.rt
cp "$JB_TOOLS_DIR"/common/modular-sdk-build-properties.txt "$MODULAR_SDK_DIR"/make/gluegen.rt/build.properties
