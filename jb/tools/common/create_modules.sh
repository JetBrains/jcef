# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

# shellcheck source=common.sh
source "$JB_TOOLS_DIR"/common/common.sh

export JOGAMP_DIR="$JCEF_ROOT_DIR"/third_party/jogamp/jar

function extract_jar {
  __jar=$1
  __dst_dir=$2
  __content_dir=$3
  __tmp=.tmp_extract_jar

  clean_mkdir_cd $__tmp || exit 1
  cp "$__jar" . || exit 1

  "$JAVA_HOME"/bin/jar -xf ./*.jar || exit 1
  rm ./*.jar
  rm -rf META-INF
  cd ..

  clean_mkdir "$__dst_dir" || exit 1
  if [ -z "$__content_dir" ]
  then
      cp -R $__tmp/* "$__dst_dir" || exit 1
  else
      cp -R $__tmp/"$__content_dir"/* "$__dst_dir" || exit 1
  fi

  rm -rf $__tmp
}

cd "$JCEF_ROOT_DIR" || exit 1
clean_mkdir_cd jmods || exit 1

echo "*** create gluegen.rt module..."
cp "$JOGAMP_DIR"/gluegen-rt.jar . || exit 1
cp "$JB_TOOLS_DIR"/common/gluegen-module-info.java module-info.java || exit 1

"$JAVA_HOME"/bin/javac --patch-module gluegen.rt=gluegen-rt.jar module-info.java || exit 1
"$JAVA_HOME"/bin/jar uf gluegen-rt.jar module-info.class || exit 1

rm module-info.class module-info.java || exit 1

mkdir lib || exit 1
extract_jar "$JOGAMP_DIR"/gluegen-rt-natives-"$OS"-"$ARCH".jar lib natives/"$OS"-"$ARCH" || exit 1

"$JAVA_HOME"/bin/jmod create --class-path gluegen-rt.jar --libs lib gluegen.rt.jmod || exit 1
rm -rf gluegen-rt.jar lib || exit 1

echo "*** create jogl.all module..."
cp "$JOGAMP_DIR"/jogl-all.jar . || exit 1
cp "$JB_TOOLS_OS_DIR"/jogl-module-info.java module-info.java || exit 1

"$JAVA_HOME"/bin/javac --module-path . --patch-module jogl.all=jogl-all.jar module-info.java || exit 1
"$JAVA_HOME"/bin/jar uf jogl-all.jar module-info.class || exit 1

rm module-info.class module-info.java || exit 1

mkdir lib || exit 1
extract_jar "$JOGAMP_DIR"/jogl-all-natives-"$OS"-"$ARCH".jar lib natives/"$OS"-"$ARCH" || exit 1

"$JAVA_HOME"/bin/jmod create --module-path . --class-path jogl-all.jar --libs lib jogl.all.jmod || exit 1
rm -rf jogl-all.jar lib || exit 1

echo "*** create jcef module..."
cp "$OUT_CLS_DIR"/jcef.jar . || exit 1

# shellcheck disable=SC2010
case "$OS" in
  "windows")
    mkdir bin || exit 1
    for resource in $(ls "$OUT_NATIVE_DIR" | grep '\.dat\|\.exe\|\.bin')
    do
      cp -R "$OUT_NATIVE_DIR"/"$resource" bin || exit 1
    done

    mkdir lib || exit 1
    for resource in $(ls "$OUT_NATIVE_DIR" | grep -v '\.dat\|\.exe\|\.bin\|\.exp\|\.lib')
    do
      cp -R "$OUT_NATIVE_DIR"/"$resource" lib || exit 1
    done

    "$JAVA_HOME"/bin/jmod create --module-path . --class-path jcef.jar --cmds bin --libs lib jcef.jmod || exit 1
    rm -rf jcef.jar bin lib || exit 1
    ;;

  "macosx")
    mkdir lib || exit 1
    cp "$OUT_NATIVE_DIR"/libjcef.dylib lib || exit 1

    "$JAVA_HOME"/bin/jmod create --module-path . --class-path jcef.jar --libs lib jcef.jmod || exit 1
    rm -rf jcef.jar lib || exit 1
    ;;

  "linux")
    mkdir lib || exit 1
    cp -R "$OUT_NATIVE_DIR"/* lib || exit 1

    echo "*** find patched libcef.so..."
    if [ -z "$PATCHED_LIBCEF_DIR" ]; then
        echo "warning: PATCHED_LIBCEF_DIR is not set, then using current dir"
        PATCHED_LIBCEF_DIR=$(pwd)
    fi
    if [ -f "$PATCHED_LIBCEF_DIR/libcef.so" ]; then
        cp "$PATCHED_LIBCEF_DIR"/libcef.so lib || exit 1
    else
        echo "warning: $PATCHED_LIBCEF_DIR/libcef.so does not exist, then bundling stock libcef.so"
    fi

    echo "*** strip..."
    # shellcheck disable=SC2038
    find lib -name "*.so" | xargs strip -x || exit 1
    strip -x lib/chrome-sandbox || exit 1
    strip -x lib/jcef_helper || exit 1

    "$JAVA_HOME"/bin/jmod create --module-path . --class-path jcef.jar --libs lib jcef.jmod || exit 1
    rm -rf jcef.jar lib || exit 1
    ;;
esac

cd ..
