# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

# DIR_ARM64 and DIR_X64 must have the same structure
# How to run the script:
# $ ls -l
# DIR_ARM64
# DIR_X64
# $ cd DIR_ARM64
# $ create_universal . ../DIR_X64

export DIR_ARM64="$1"
export DIR_X64="$2"

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

find "$DIR_ARM64" -type f -exec bash -c 'test_arm64 $@' _ {} \; -exec bash -c 'create_fat $@' _ {} \;
