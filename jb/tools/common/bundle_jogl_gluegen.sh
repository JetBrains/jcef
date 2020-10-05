# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

export JOGAMP_DIR=$JCEF_ROOT_DIR/third_party/jogamp/jar

function extract_jar {
  __jar=$1
  __dst_dir=$2
  __content_dir=$3

  [ -d "__tmp" ] && rm -rf __tmp
  mkdir __tmp
  cp $__jar __tmp || exit 1
  cd __tmp

  $JAVA_HOME/bin/jar -xf *.jar || exit 1
  rm *.jar
  rm -rf META-INF

  mkdir -p $__dst_dir
  if [ -z "$__content_dir" ]
  then
        cp -R * $__dst_dir || exit 1
  else
        cp -R $__content_dir/* $__dst_dir || exit 1
  fi

  cd ..
  rm -rf __tmp
}

cd $JCEF_ROOT_DIR

echo "*** bundle jogl and gluegen modules..."
extract_jar $JOGAMP_DIR/jogl-all.jar $MODULAR_SDK_DIR/modules/jogl.all
extract_jar $JOGAMP_DIR/gluegen-rt.jar $MODULAR_SDK_DIR/modules/gluegen.rt

echo "*** bundle jogl and gluegen modules_libs..."
extract_jar $JOGAMP_DIR/jogl-all-natives-windows-amd64.jar $MODULAR_SDK_DIR/modules_libs/jogl.all natives/${OS}-${ARCH}
extract_jar $JOGAMP_DIR/gluegen-rt-natives-windows-amd64.jar $MODULAR_SDK_DIR/modules_libs/gluegen.rt natives/${OS}-${ARCH}

echo "*** bundle jogl and gluegen modules_src..."
mkdir -p $MODULAR_SDK_DIR/modules_src/jogl.all
cp $JB_TOOLS_DIR/${OS}/jogl-module-info-java.txt $MODULAR_SDK_DIR/modules_src/jogl.all/module-info.java

mkdir -p $MODULAR_SDK_DIR/modules_src/gluegen.rt
cp $JB_TOOLS_DIR/${OS}/gluegen-module-info-java.txt $MODULAR_SDK_DIR/modules_src/gluegen.rt/module-info.java

echo "*** bundle jogl and gluegen make..."
mkdir -p $MODULAR_SDK_DIR/make/jogl.all
cp $JB_TOOLS_DIR/common/modular-sdk-build-properties.txt $MODULAR_SDK_DIR/make/jogl.all/build.properties

mkdir -p $MODULAR_SDK_DIR/make/gluegen.rt
cp $JB_TOOLS_DIR/common/modular-sdk-build-properties.txt $MODULAR_SDK_DIR/make/gluegen.rt/build.properties
