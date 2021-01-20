# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if ! source set_env.sh
then
    exit 1
fi

function test_arm64 {
  __file=$*

  file "$__file" | grep 'Mach-O' > /dev/null || return 1
  lipo -info "$__file" | grep 'Non-fat file' | grep 'arm64' > /dev/null || return 1

  return 0
}
export -f test_arm64

function create_fat {
  __executable=$*

  lipo -info "$__executable"
  mv "$__executable" "${__executable}"_tmp
  lipo -create -output "$__executable" "${__executable}"_tmp "$DIR_X64"/"$__executable"
  rm "${__executable}"_tmp
}
export -f create_fat

function build_arch {
  __arch=$1

  __dir=jcef_mac_"$__arch"
  __cur_dir=$(pwd)

  bash build.sh clean || exit 1
  bash build.sh all "$__arch" || exit 1

  rm -rf "${JCEF_ROOT_DIR:?}"/"$__dir" || exit 1
  mkdir "$JCEF_ROOT_DIR"/"$__dir" || exit 1
  mv "$JCEF_ROOT_DIR"/jcef_mac.tar.gz "$JCEF_ROOT_DIR"/"$__dir" || exit 1

  cd "$JCEF_ROOT_DIR"/"$__dir" || exit 1
  tar xzf jcef_mac.tar.gz || exit 1
  rm jcef_mac.tar.gz || exit 1

  cd "$__cur_dir" || exit 1
}

build_arch arm64 || exit 1
build_arch x86_64 || exit 1

export DIR_X64="$JCEF_ROOT_DIR"/jcef_mac_x86_64
cd "$JCEF_ROOT_DIR"/jcef_mac_arm64 || exit 1
find . -type f -exec bash -c 'test_arm64 $@' _ {} \; -exec bash -c 'create_fat $@' _ {} \; || exit 1

cd "$JCEF_ROOT_DIR" || exit 1
tar czf jcef_mac_universal.tar.gz jcef_mac_arm64 || exit 1

rm -rf jcef_mac_arm64 || exit 1
rm -rf jcef_mac_x86_64 || exit 1

cd "$JB_TOOLS_OS_DIR" || exit 1
