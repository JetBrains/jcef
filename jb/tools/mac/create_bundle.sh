# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

RELEASE_PATH="$JCEF_ROOT_DIR"/jcef_build/native/Release
ARTIFACT=jcef_mac

echo "*** delete $ARTIFACT..."
rm -rf "${JCEF_ROOT_DIR:?}/$ARTIFACT"
rm -f "${JCEF_ROOT_DIR:?}/$ARTIFACT.tar.gz"

if [ "$1" == "clean" ]; then
    exit 0
fi

echo "*** copy binaries..."
mkdir "$ARTIFACT"
cp -R "$MODULAR_SDK_DIR" "$ARTIFACT"/
cp -R "$RELEASE_PATH"/jcef_app.app/Contents/Frameworks "$ARTIFACT"/

echo "*** create bundle..."

bash "$JB_TOOLS_DIR"/common/bundle_jogl_gluegen.sh

# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz" || exit 1

echo "*** SUCCESSFUL"
cd "$JB_TOOLS_OS_DIR" || exit 1