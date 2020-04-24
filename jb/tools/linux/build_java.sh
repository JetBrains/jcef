# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

OUT_DIR=$JCEF_ROOT_DIR/out

if [ "$1" == "clean" ]; then
    echo "*** delete $OUT_DIR..."
    rm -rf "$OUT_DIR"
    exit 0
fi
mkdir -p "$OUT_DIR"

echo "*** compile java sources..."
cd "$JCEF_ROOT_DIR"/tools || exit 1
./compile.sh linux64 Release || exit 1

cd "$JB_TOOLS_LINUX_DIR" || exit 1
