# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

OUT_DIR=$JCEF_ROOT_DIR/jcef_build
OUT_JAVA_DIR=$JCEF_ROOT_DIR/out

if [ "$1" == "clean" ]; then
    echo "*** delete $OUT_DIR..."
    rm -rf "$OUT_DIR"
    echo "*** delete $OUT_JAVA_DIR..."
    rm -rf "$OUT_JAVA_DIR"
    exit 0
fi
mkdir -p "$OUT_DIR"
mkdir -p "$OUT_JAVA_DIR"

case "$2" in
    "arm64")
        export TARGET_ARCH="arm64"
        ;;
    "x86_64")
        export TARGET_ARCH="x86_64"
        ;;
esac

echo "*** create modular jogl..."
cd "$JB_TOOLS_DIR" || exit 1
bash ./modular-jogl.sh || exit 1

echo "*** run cmake [TARGET=$TARGET_ARCH]..."
cd "$OUT_DIR" || exit 1
cmake -G "Xcode" -DPROJECT_ARCH="$TARGET_ARCH" .. || exit 1

echo "*** run xcodebuild..."
xcodebuild -configuration Release || exit 1

echo "*** change @rpath in libjcef.dylib..."
cd "$JCEF_ROOT_DIR"/jcef_build/native/Release || exit 1
install_name_tool -change @rpath/libjvm.dylib @loader_path/server/libjvm.dylib libjcef.dylib || exit 1
install_name_tool -change @rpath/libjawt.dylib @loader_path/libjawt.dylib libjcef.dylib || exit 1

JNF_RPATH=$(otool -L libjcef.dylib | grep JavaNativeFoundation | awk '{print $1}')
test -z "$JNF_RPATH" || install_name_tool -change "$JNF_RPATH" @loader_path/../../Frameworks/JavaNativeFoundation.framework/JavaNativeFoundation libjcef.dylib || exit 1

cp libjcef.dylib modular-sdk/modules_libs/jcef/
cp libjcef.dylib jcef_app.app/Contents/Java/

cd "$JB_TOOLS_OS_DIR" || exit 1
