# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

echo "*** compile java sources..."
cd "$JCEF_ROOT_DIT"/tools || exit 1
./compile.sh linux64 Release

cd "$JB_TOOLS_LINUX_DIR" || exit 1
