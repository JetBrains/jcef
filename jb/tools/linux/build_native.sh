# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh
if [ $? -ne 0 ]; then
  exit 1
fi
cd ../../..

# workaround python failure in docker
git checkout tools/make_version_header.py

echo "*** create modular jogl..."
chmod u+x ./jb/tools/modular-jogl.sh
./jb/tools/modular-jogl.sh

echo "*** run cmake..."
mkdir jcef_build
cd jcef_build
cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release ..

echo "*** run make..."
make -j4

cd ../$JB_TOOLS_LINUX_DIR