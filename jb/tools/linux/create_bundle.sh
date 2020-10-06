# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

ARTIFACT=jcef_linux_x64

echo "*** delete $ARTIFACT..."
rm -rf "${JCEF_ROOT_DIR:?}/$ARTIFACT"
rm -f "${JCEF_ROOT_DIR:?}/$ARTIFACT.tar.gz"

if [ "$1" == "clean" ]; then
    exit 0
fi

cd "$JCEF_ROOT_DIR" || exit 1

echo "*** bundle jogl and gluegen..."
bash "$JB_TOOLS_DIR"/common/bundle_jogl_gluegen.sh || exit 1

echo "*** copy jcef binaries..."
mkdir "$ARTIFACT"
cp -R jcef_build/native/Release/* "$ARTIFACT"/
cp -R "$MODULAR_SDK_DIR" "$ARTIFACT"/

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

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz" || exit 1

echo "*** SUCCESSFUL"
cd "$JB_TOOLS_OS_DIR" || exit 1
