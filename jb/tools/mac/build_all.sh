# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if [ ! -f "./build_all.sh" ]; then
    echo "error: not in jb tools mac dir"
    exit 1
fi

PATH=$(pwd):$PATH
export PATH

echo -e "\n*** BUILD NATIVE/JAVA ***\n"
bash build_native_java.sh || exit 1

echo -e "\n*** CREATE BUNDLE ***\n"
bash create_bundle.sh || exit 1
