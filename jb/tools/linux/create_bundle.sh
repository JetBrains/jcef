# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi
cd "$JCEF_ROOT_DIR" || exit 1

MODULAR_SDK="$JCEF_ROOT_DIR"/out/linux64/modular-sdk

echo "*** temp exclude jogl from modular-info.java..."
# shellcheck disable=SC2002
cat "$MODULAR_SDK"/modules_src/jcef/module-info.java | grep -v jogl | grep -v gluegen > __tmp
mv __tmp "$MODULAR_SDK"/modules_src/jcef/module-info.java

echo "*** copy jcef binaries..."
mkdir jcef_linux_x64
cp -R jcef_build/native/Release/* jcef_linux_x64/
cp -R "$MODULAR_SDK" jcef_linux_x64/

echo "*** copy patched libcef.so..."
if [ -z "$PATCHED_LIBCEF_DIR" ]; then
    PATCHED_LIBCEF_DIR=$(pwd)
fi
if [ ! -f "$PATCHED_LIBCEF_DIR/libcef.so" ]; then
    echo "error: $PATCHED_LIBCEF_DIR/libcef.so dos not exist"
    exit 1
fi
cp "$PATCHED_LIBCEF_DIR"/libcef.so jcef_linux_x64/

echo "*** strip..."
# shellcheck disable=SC2038
find jcef_linux_x64 -name "*.so" | xargs strip -x
strip -x jcef_linux_x64/chrome-sandbox
strip -x jcef_linux_x64/jcef_helper

echo "*** create bundle..."
rm -f jcef_linux_x64.tar.gz
# shellcheck disable=SC2046
tar -cvzf jcef_linux_x64.tar.gz -C jcef_linux_x64 $(ls jcef_linux_x64)
rm -rf jcef_linux_x64
ls -lah jcef_linux_x64.tar.gz

cd "$JB_TOOLS_LINUX_DIR" || exit 1