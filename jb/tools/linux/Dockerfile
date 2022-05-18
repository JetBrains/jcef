FROM ubuntu:xenial

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y software-properties-common apt-transport-https apt-utils

RUN apt-get install -y build-essential zip bzip2 unzip tar wget make autoconf \
  automake libtool gcc libc++-dev libasound2-dev libcups2-dev libx11-dev \
  libjpeg-turbo8-dev libgif-dev libfreetype6-dev file libxtst-dev libxt-dev \
  libxrender-dev libfontconfig1-dev libgtk2.0-dev libatk-adaptor \
  git ant python3 mesa-common-dev libxkbcommon0 libgbm1

RUN wget -nv -O jdk.deb https://cache-redirector.jetbrains.com/corretto.aws/downloads/resources/11.0.15.9.1/java-11-amazon-corretto-jdk_11.0.15.9-1_$(dpkg-architecture -q DEB_BUILD_ARCH).deb && \
    dpkg -i jdk.deb
ENV JDK_11=/usr/lib/jvm/java-11-amazon-corretto
RUN ${JDK_11}/bin/javac -version

RUN wget -nv -O /cmake.tar.gz https://github.com/Kitware/CMake/releases/download/v3.23.1/cmake-3.23.1-linux-$(uname -m).tar.gz && \
    tar xfz /cmake.tar.gz --strip-components=1 -C /usr/local && \
    rm /cmake.tar.gz && \
    cmake --version
