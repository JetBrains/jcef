# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh || exit 1

# shellcheck source=../common/common.sh
source "$JB_TOOLS_DIR"/common/common.sh || exit 1

ARTIFACT=jcef_linux_x64

echo "*** delete $ARTIFACT..."
rm -rf "${JCEF_ROOT_DIR:?}/$ARTIFACT"
rm -f "${JCEF_ROOT_DIR:?}/$ARTIFACT.tar.gz"

if [ "$1" == "clean" ]; then
    exit 0
fi

cd "$JCEF_ROOT_DIR" || do_fail

echo "*** create modules..."
bash "$JB_TOOLS_DIR"/common/create_modules.sh || do_fail

echo "*** copy binaries..."
clean_mkdir $ARTIFACT || do_fail
mv jmods $ARTIFACT || do_fail

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf $ARTIFACT.tar.gz -C $ARTIFACT $(ls $ARTIFACT) || do_fail
rm -rf ./$ARTIFACT || do_fail
ls -lah $ARTIFACT.tar.gz || do_fail

cd "$JB_TOOLS_OS_DIR" || do_fail
