# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

echo "*** create modular jogl..."
cd "$JB_TOOLS_DIR" || exit 1
bash ./modular-jogl.sh || exit 1

echo "*** run cmake..."
cd "$JCEF_ROOT_DIR" || exit 1
mkdir jcef_build
cd jcef_build || exit 1

cmake -G "Xcode" -DPROJECT_ARCH="x86_64" ..

echo "*** run xcodebuild..."
xcodebuild -configuration Release

echo "*** change @rpath in libjcef.dylib..."
cd "$JCEF_ROOT_DIR"/jcef_build/native/Release || exit 1
install_name_tool -change @rpath/libjvm.dylib @loader_path/server/libjvm.dylib libjcef.dylib
install_name_tool -change @rpath/libjawt.dylib @loader_path/libjawt.dylib libjcef.dylib

cp libjcef.dylib modular-sdk/modules_libs/jcef/
cp libjcef.dylib jcef_app.app/Contents/Java/

cd "$JB_TOOLS_MAC_DIR" || exit 1
