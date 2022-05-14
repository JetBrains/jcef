#!/bin/bash
set -exuo pipefail

# installed in Dockerfile
export JDK_11=/usr/lib/jvm/java-11-openjdk-amd64
"$JDK_11/bin/java" -version

echo "*** build all..."

case $(uname -m) in
  x86_64)
    arch=x86_64
    cef_platform=64
    ;;
  aarch64)
    arch=arm64
    cef_platform=arm64
    ;;
  *)
    echo "Unsupported arch: $(uname -m)"
    exit 1
    ;;
esac

cef_dir=../../../third_party/cef

shopt -s nullglob
cef_distributions=($cef_dir/cef_binary_*_linux${cef_platform}_minimal.zip)
echo ${#cef_distributions[@]}
if [ ${#cef_distributions[@]} != 1 ]; then
  echo "ERROR Expected one and only one CEF distribution file at $cef_dir" >&2
  ls -la "$cef_dir"
  exit 1
fi
cef_dist=$(basename ${cef_distributions[0]})
echo "Matched CEF distribution $cef_dist"

[[ $cef_dist =~ cef_binary_(.*)_linux${cef_platform}_minimal\.zip ]]
export CEF_VERSION="${BASH_REMATCH[1]}"

echo "CEF version is $CEF_VERSION"

# CEF is already downloaded, do not download another one in any case
export CEF_DONT_DOWNLOAD=true

bash build.sh all "$arch"
