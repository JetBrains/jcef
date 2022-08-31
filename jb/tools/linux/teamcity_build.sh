#!/bin/bash
set -exuo pipefail

# to avoid error:
# fatal: detected dubious ownership in repository at '/mnt/agent/work/4257c71f6987934f'
git config --global --add safe.directory "$(pwd)"

# CD to script's directory
cd "$(dirname "$0")"

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

if [ -n "${CEF_USE_LOCAL:-}" ]; then
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
else
  echo "CEF distribution will be downloaded during building"
fi

echo "*** build all..."
bash build.sh all "$arch"
