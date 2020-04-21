# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi
cd "$JCEF_ROOT_DIR" || exit 1

# workaround python failure in docker
git checkout tools/make_version_header.py

echo "*** create modular jogl..."
cd "$JB_TOOLS_DIR" || exit 1
bash ./modular-jogl.sh || exit 1

echo "*** run cmake..."
cd "$JCEF_ROOT_DIR" || exit 1
mkdir jcef_build
cd jcef_build || exit 1
cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release ..

echo "*** run make..."
make -j4

cd "$JB_TOOLS_LINUX_DIR" || exit 1