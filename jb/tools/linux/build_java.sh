# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh
if [ $? -ne 0 ]; then
  exit 1
fi
cd ../../..

echo "*** compile sources..."
cd tools
./compile.sh linux64 Release

cd ../$JB_TOOLS_LINUX_DIR
