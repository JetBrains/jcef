# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

ARTIFACT=jcef_mac

echo "*** delete $ARTIFACT..."
rm -rf "$ARTIFACT"
rm -f "$ARTIFACT.*"

if [ "$1" == "clear" ]; then
    exit 0
fi

RELEASE_PATH="$JCEF_ROOT_DIR"/jcef_build/native/Release

echo "*** temp exclude jogl..."
cd "$JCEF_ROOT_DIR" || exit 1
# shellcheck disable=SC2002
cat "$RELEASE_PATH"/modular-sdk/modules_src/jcef/module-info.java | grep -v jogl\.all | grep -v gluegen\.rt > __tmp
mv __tmp "$RELEASE_PATH"/modular-sdk/modules_src/jcef/module-info.java

echo "*** copy binaries..."
mkdir "$ARTIFACT"
cp -R "$RELEASE_PATH"/modular-sdk "$ARTIFACT"/
cp -R "$RELEASE_PATH"/jcef_app.app/Contents/Frameworks "$ARTIFACT"/

echo "*** create bundle..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz"

echo "*** SUCCESSFUL"
cd "$JB_TOOLS_MAC_DIR" || exit 1