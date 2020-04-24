# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

ARTIFACT=jcef_linux_x64

echo "*** delete $ARTIFACT..."
rm -rf "$ARTIFACT"
rm -f "$ARTIFACT.*"

if [ "$1" == "clean" ]; then
    exit 0
fi

cd "$JCEF_ROOT_DIR" || exit 1

MODULAR_SDK="$JCEF_ROOT_DIR"/out/linux64/modular-sdk

echo "*** temp exclude jogl from modular-info.java..."
# shellcheck disable=SC2002
cat "$MODULAR_SDK"/modules_src/jcef/module-info.java | grep -v jogl | grep -v gluegen > __tmp
mv __tmp "$MODULAR_SDK"/modules_src/jcef/module-info.java

echo "*** copy jcef binaries..."
mkdir "$ARTIFACT"
cp -R jcef_build/native/Release/* "$ARTIFACT"/
cp -R "$MODULAR_SDK" "$ARTIFACT"/

echo "*** copy patched libcef.so..."
if [ -z "$PATCHED_LIBCEF_DIR" ]; then
    echo "warning: PATCHED_LIBCEF_DIR is not set. Current dir will be used"
    PATCHED_LIBCEF_DIR=$(pwd)
fi
if [ ! -f "$PATCHED_LIBCEF_DIR/libcef.so" ]; then
    echo "warning: $PATCHED_LIBCEF_DIR/libcef.so does not exist. Stock libcef.so will be used"
    PATCHED_LIBCEF_DIR="$JCEF_ROOT_DIR"/jcef_build/native/Release
fi
cp "$PATCHED_LIBCEF_DIR"/libcef.so "$ARTIFACT"/

echo "*** strip..."
# shellcheck disable=SC2038
find "$ARTIFACT" -name "*.so" | xargs strip -x
strip -x "$ARTIFACT"/chrome-sandbox
strip -x "$ARTIFACT"/jcef_helper

echo "*** create bundle..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz" || exit 1

echo "*** SUCCESSFUL"
cd "$JB_TOOLS_LINUX_DIR" || exit 1
