# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh || exit 1

# shellcheck source=../common/common.sh
source "$JB_TOOLS_DIR"/common/common.sh || exit 1

OUT_DIR=$JCEF_ROOT_DIR/out

if [ "$1" == "clean" ]; then
    echo "*** delete $OUT_DIR..."
    rm -rf "$OUT_DIR"
    exit 0
fi
clean_mkdir "$OUT_DIR" || do_fail

echo "*** compile java sources..."
cd "$JCEF_ROOT_DIR"/tools || do_fail
./compile.sh linux64 Release || do_fail

cd "$JB_TOOLS_OS_DIR" || do_fail
