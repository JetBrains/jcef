#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

export JOGAMP_DIR="$JCEF_ROOT_DIR"/third_party/jogamp/jar
export THRIFT_DIR="$JCEF_ROOT_DIR"/third_party/thrift
export SLF_DIR="$JCEF_ROOT_DIR"/third_party/slf4j
export THRIFT_JAR=libthrift-0.19.0.jar
export SLF_JAR=slf4j-api-2.0.0.jar

function extract_jar {
  __jar=$1
  __dst_dir=$2
  __content_dir="${3:-.}"
  __tmp=.tmp_extract_jar

  rm -rf "$__tmp" && mkdir "$__tmp"

  (
    cd "$__tmp" || exit 1
    "$JAVA_HOME"/bin/jar -xf "$__jar"
  )
  rm -rf "$__tmp/META-INF"

  rm -rf "$__dst_dir" && mkdir "$__dst_dir"
  if [ -z "$__content_dir" ]
  then
      cp -R "$__tmp"/* "$__dst_dir"
  else
      cp -R "$__tmp"/"$__content_dir"/* "$__dst_dir"
  fi

  rm -rf $__tmp
}

cd "$JCEF_ROOT_DIR" || exit 1
rm -rf jmods && mkdir jmods
cd jmods || exit 1

echo "*** create gluegen.rt module..."
cp "$JOGAMP_DIR"/gluegen-rt.jar .
cp "$JB_TOOLS_DIR"/common/gluegen-module-info.java module-info.java

"$JAVA_HOME"/bin/javac --patch-module gluegen.rt=gluegen-rt.jar module-info.java
"$JAVA_HOME"/bin/jar uf gluegen-rt.jar module-info.class

rm module-info.class module-info.java

mkdir lib
if [ "${OS}" == "macosx" ] || [ "${TARGET_ARCH}" == "x86_64" ]; then
    extract_jar "$JOGAMP_DIR"/gluegen-rt-natives-"$OS"-"$DEPS_ARCH".jar lib natives/"$OS"-"$DEPS_ARCH"
fi
"$JAVA_HOME"/bin/jmod create --class-path gluegen-rt.jar --libs lib gluegen.rt.jmod
rm -rf gluegen-rt.jar lib

echo "*** create jogl.all module..."
cp "$JOGAMP_DIR"/jogl-all.jar .
cp "$JB_TOOLS_OS_DIR"/jogl-module-info.java module-info.java

"$JAVA_HOME"/bin/javac --module-path . --patch-module jogl.all=jogl-all.jar module-info.java
"$JAVA_HOME"/bin/jar uf jogl-all.jar module-info.class

rm module-info.class module-info.java

mkdir lib
if [ "${OS}" == "macosx" ] || [ "${TARGET_ARCH}" == "x86_64" ]; then
    extract_jar "$JOGAMP_DIR"/jogl-all-natives-"$OS"-"$DEPS_ARCH".jar lib natives/"$OS"-"$DEPS_ARCH"
fi
"$JAVA_HOME"/bin/jmod create --module-path . --class-path jogl-all.jar --libs lib jogl.all.jmod
rm -rf jogl-all.jar lib

echo "*** create slf4j module..."

cp "$SLF_DIR"/"$SLF_JAR" .
cp "$JB_TOOLS_DIR"/common/slf4j-module-info.java module-info.java
"$JAVA_HOME"/bin/javac --patch-module org.slf4j=$SLF_JAR module-info.java

export TMP_DIR="tmp_jar_content"
mkdir $TMP_DIR
cd $TMP_DIR
"$JAVA_HOME"/bin/jar -xvf ../$SLF_JAR
rm -rf ./META-INF/versions
cp ../module-info.class .
"$JAVA_HOME"/bin/jar -cvf ../slf4j.jar .
cd ..
rm -rf module-info.class module-info.java $TMP_DIR
"$JAVA_HOME"/bin/jmod create --class-path slf4j.jar org.slf4j.jmod

echo "*** create thrift module..."

cp "$THRIFT_DIR"/"$THRIFT_JAR" .
cp "$JB_TOOLS_DIR"/common/thrift-module-info.java module-info.java
"$JAVA_HOME"/bin/javac --module-path="$SLF_DIR"/"$SLF_JAR" --patch-module org.apache.thrift=$THRIFT_JAR module-info.java
"$JAVA_HOME"/bin/jar uf $THRIFT_JAR module-info.class
rm module-info.class module-info.java
"$JAVA_HOME"/bin/jmod create --class-path $THRIFT_JAR org.apache.thrift.jmod
rm -rf "$THRIFT_JAR"
rm -rf "$SLF_JAR" slf4j.jar

echo "*** create jcef module..."
cp "$OUT_CLS_DIR"/jcef.jar .

# shellcheck disable=SC2010
case "$OS" in
"windows")
  mkdir bin
  for resource in $(ls "$OUT_NATIVE_DIR" | grep '\.dat\|\.exe\|\.bin'); do
    cp -R "$OUT_NATIVE_DIR"/"$resource" bin
  done

  mkdir lib
  for resource in $(ls "$OUT_NATIVE_DIR" | grep -v '\.dat\|\.exe\|\.bin\|\.exp\|\.lib'); do
    # TODO: remove resource dups
    cp -R "$OUT_NATIVE_DIR"/"$resource" lib
  done

  if [[ -n "${OUT_REMOTE_DIR-}" ]]; then
    echo "Coping $OUT_REMOTE_DIR/bin/cef_server.exe and $OUT_REMOTE_DIR/shared_mem_helper.dll to lib"
    cp -R "$OUT_REMOTE_DIR"/bin/cef_server.exe bin
    cp -R "$OUT_REMOTE_DIR"/shared_mem_helper.dll lib
  fi

  "$JAVA_HOME"/bin/jmod create --module-path . --class-path jcef.jar --cmds bin --libs lib jcef.jmod
  rm -rf jcef.jar bin lib

  rm -rf ../cef_server && mkdir ../cef_server
  cp -R "$OUT_REMOTE_DIR"/bin ../cef_server
  cp -R "$OUT_REMOTE_DIR"/lib ../cef_server
  cp "$OUT_REMOTE_DIR"/shared_mem_helper.dll ../cef_server
  ;;

"macosx")
  mkdir lib
  cp "$OUT_NATIVE_DIR"/libjcef.dylib lib
  cp "$OUT_REMOTE_DIR"/libshared_mem_helper.dylib lib

  "$JAVA_HOME"/bin/jmod create --module-path . --class-path jcef.jar --libs lib jcef.jmod
  rm -rf jcef.jar lib
  ;;

"linux")
  mkdir lib
  cp -R "$OUT_NATIVE_DIR"/* lib
  cp -R "$OUT_REMOTE_DIR"/libshared_mem_helper.so lib
  cp -R "$OUT_REMOTE_DIR"/cef_server lib

  echo "*** create cef_server bundle..."
  rm -rf ../cef_server && mkdir ../cef_server
  cp -R "$OUT_REMOTE_DIR"/* ../cef_server
  find ../cef_server -name "*.log" -type f -delete

  echo "*** find patched libcef.so..."
  if [ -z "${PATCHED_LIBCEF_DIR:-}" ]; then
    echo "warning: PATCHED_LIBCEF_DIR is not set, then using current dir ($(pwd))"
    PATCHED_LIBCEF_DIR=$(pwd)
  fi
  if [ -f "$PATCHED_LIBCEF_DIR/libcef.so" ]; then
    cp "$PATCHED_LIBCEF_DIR"/libcef.so lib
  else
    echo "warning: $PATCHED_LIBCEF_DIR/libcef.so does not exist, then bundling stock libcef.so"
  fi

  echo "*** strip..."
  # shellcheck disable=SC2038
  find lib -name "*.so" | xargs strip -x
  strip -x lib/chrome-sandbox
  strip -x lib/jcef_helper
  strip -x lib/cef_server

  "$JAVA_HOME"/bin/jmod create --module-path . --class-path jcef.jar --libs lib jcef.jmod
  rm -rf jcef.jar lib
  ;;
esac

cd ..
