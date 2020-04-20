# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

export PATH=`pwd`:$PATH

echo -e "\n*** BUILD NATIVE ***\n"
build_native.sh || exit 1

echo -e "\n*** BUILD JAVA ***\n"
build_java.sh || exit 1

echo -e "\n*** CREATE BUNDLE ***\n"
create_bundle.sh || exit 1
