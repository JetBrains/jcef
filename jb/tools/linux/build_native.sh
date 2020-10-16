# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh || exit 1

# shellcheck source=../common/common.sh
source "$JB_TOOLS_DIR"/common/common.sh || exit 1

OUT_DIR=$JCEF_ROOT_DIR/jcef_build

if [ "$1" == "clean" ]; then
    echo "*** delete $OUT_DIR..."
    rm -rf "$OUT_DIR"
    exit 0
fi
clean_mkdir "$OUT_DIR" || do_fail

cd "$JCEF_ROOT_DIR" || do_fail

# workaround python failure in docker
git checkout tools/make_version_header.py || do_fail

echo "*** run cmake..."
cd "$OUT_DIR" || do_fail
cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release .. || do_fail

echo "*** run make..."
make -j4 || do_fail

cd "$JB_TOOLS_OS_DIR" || do_fail