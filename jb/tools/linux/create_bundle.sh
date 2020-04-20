# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh
if [ $? -ne 0 ]; then
    exit 1
fi
cd ../../..

MODULAR_SDK=out/linux64/modular-sdk

echo "*** temp exclude jogl from modular-info.java..."
cat $MODULAR_SDK/modules_src/jcef/module-info.java | grep -v jogl | grep -v gluegen > __tmp
mv __tmp $MODULAR_SDK/modules_src/jcef/module-info.java

echo "*** copy jcef binaries..."
mkdir jcef_linux_x64
cp -R jcef_build/native/Release/* jcef_linux_x64/
cp -R $MODULAR_SDK jcef_linux_x64/

echo "*** copy patched libcef.so..."
if [ -z "$PATCHED_LIBCEF_DIR" ]; then
    PATCHED_LIBCEF_DIR=`pwd`
fi
if [ ! -f "$PATCHED_LIBCEF_DIR/libcef.so" ]; then
    echo "error: $PATCHED_LIBCEF_DIR/libcef.so dos not exist"
    exit 1
fi
cp $PATCHED_LIBCEF_DIR/libcef.so jcef_linux_x64/

echo "*** strip..."
find jcef_linux_x64 -name "*.so" | xargs strip -x
strip -x jcef_linux_x64/chrome-sandbox
strip -x jcef_linux_x64/jcef_helper

echo "*** create bundle..."
rm -f jcef_linux_x64.tar.gz
tar -cvzf jcef_linux_x64.tar.gz -C jcef_linux_x64 $(ls jcef_linux_x64)
rm -rf jcef_linux_x64
ls -lah jcef_linux_x64.tar.gz

cd $JB_TOOLS_LINUX_DIR