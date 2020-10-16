# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

function clean_mkdir() {
    __dir=$1

    [ -d "$__dir" ] && rm -rf "$__dir"
    mkdir "$__dir" || return 1
}

function clean_mkdir_cd() {
    __dir=$1

    clean_mkdir "$__dir" || return 1
    cd "$__dir" || return 1
}

function do_fail() {
  echo "*** BUILD FAILED"
  # shellcheck disable=SC2164
  cd "$JB_TOOLS_OS_DIR"
  exit 1
}
