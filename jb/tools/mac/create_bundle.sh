# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

RELEASE_PATH="$JCEF_ROOT_DIR"/jcef_build/native/Release

echo "*** temp exclude jogl..."
cd "$JCEF_ROOT_DIR" || exit 1
# shellcheck disable=SC2002
cat "$RELEASE_PATH"/modular-sdk/modules_src/jcef/module-info.java | grep -v jogl\.all | grep -v gluegen\.rt > __tmp
mv __tmp "$RELEASE_PATH"/modular-sdk/modules_src/jcef/module-info.java

echo "*** copy binaries..."
mkdir jcef_mac
cp -R "$RELEASE_PATH"/modular-sdk jcef_mac/
cp -R "$RELEASE_PATH"/jcef_app.app/Contents/Frameworks jcef_mac/

echo "*** create bundle..."
rm -f jcef_mac.tar.gz
# shellcheck disable=SC2046
tar -cvzf jcef_mac.tar.gz -C jcef_mac $(ls jcef_mac)
rm -rf jcef_mac

cd "$JB_TOOLS_MAC_DIR" || exit 1