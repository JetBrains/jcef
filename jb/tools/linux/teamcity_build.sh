#!/bin/bash
set -exuo pipefail

# installed in Dockerfile
export JDK_11=/usr/lib/jvm/java-11-openjdk-amd64
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
