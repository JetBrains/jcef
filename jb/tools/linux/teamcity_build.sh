#!/bin/bash
set -exuo pipefail

echo "*** download openjdk-11..."
wget --no-check-certificate -c https://corretto.aws/downloads/latest/amazon-corretto-11-$(uname -m)-linux-jdk.tar.gz -O openjdk.tar.gz > /dev/null 2>&1
rm -rf jdk11
mkdir jdk11
tar xzf openjdk.tar.gz --strip-components 1 -C jdk11
export JDK_11="$(pwd)/jdk11"
"$JDK_11/bin/java" -version

echo "*** build all..."

case $(uname -m) in
  x86_64)
    arch=x86_64
    ;;
  aarch64)
    arch=arm64
    ;;
  *)
    echo "Unsupported arch: $(uname -m)"
    exit 1
    ;;
esac

bash build.sh all "$arch"
