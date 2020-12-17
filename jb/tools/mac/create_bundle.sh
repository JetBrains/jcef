# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

function clean {
    if [ "$1" == "legacy" ]; then
        __artefact=jcef_mac
    else
        __artefact=jcef_mac_$1
    fi
    echo "*** delete $__artefact..."
    rm -rf "${JCEF_ROOT_DIR:?}/$__artefact"
    rm -f "${JCEF_ROOT_DIR:?}/$__artefact.tar.gz"
}
export -f clean

RELEASE_PATH="$JCEF_ROOT_DIR"/jcef_build/native/Release
ARTIFACT=jcef_mac_${TARGET_ARCH}

clean arm64
clean x86_64
clean universal
clean legacy

if [ "$1" == "clean" ]; then
    exit 0
fi

cd "$JCEF_ROOT_DIR" || exit 1

echo "*** bundle jogl and gluegen..."
bash "$JB_TOOLS_DIR"/common/bundle_jogl_gluegen.sh || exit 1

echo "*** copy binaries..."
mkdir "$ARTIFACT"
cp -R "$MODULAR_SDK_DIR" "$ARTIFACT"/
cp -R "$RELEASE_PATH"/jcef_app.app/Contents/Frameworks "$ARTIFACT"/

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz" || exit 1

if [ "$TARGET_ARCH" == "x86_64" ]; then
    # preserve legacy artefact name
    cp "$ARTIFACT.tar.gz" jcef_mac.tar.gz
fi

echo "*** SUCCESSFUL"
cd "$JB_TOOLS_OS_DIR" || exit 1