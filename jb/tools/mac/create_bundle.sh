# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh || exit 1

# shellcheck source=../common/common.sh
source "$JB_TOOLS_DIR"/common/common.sh || exit 1

function clean {
    if [ "$1" == "legacy" ]; then
        __artefact=jcef_mac
    else
        __artefact=jcef_mac_$1
    fi
    if test -f "$JCEF_ROOT_DIR/$__artefact" || test -f "$JCEF_ROOT_DIR/$__artefact.tar.gz"; then
        echo "*** delete $__artefact..."
        rm -rf "${JCEF_ROOT_DIR:?}/$__artefact"
        rm -f "${JCEF_ROOT_DIR:?}/$__artefact.tar.gz"
    fi
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

cd "$JCEF_ROOT_DIR" || do_fail

echo "*** create modules..."
bash "$JB_TOOLS_DIR"/common/create_modules.sh || do_fail

echo "*** copy binaries..."
mkdir $ARTIFACT || do_fail
mv jmods $ARTIFACT || do_fail
cp -R "$RELEASE_PATH"/jcef_app.app/Contents/Frameworks $ARTIFACT || do_fail

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf $ARTIFACT.tar.gz -C $ARTIFACT $(ls $ARTIFACT) || do_fail
rm -rf ./$ARTIFACT
ls -lah $ARTIFACT.tar.gz || do_fail
cp $OUT_CLS_DIR/jcef-tests.jar . || do_fail

if [ "$TARGET_ARCH" == "x86_64" ]; then
    # preserve legacy artefact name
    cp "$ARTIFACT.tar.gz" jcef_mac.tar.gz
fi

echo "*** SUCCESSFUL"
cd "$JB_TOOLS_OS_DIR" || do_fail