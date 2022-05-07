FROM ubuntu:xenial

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y software-properties-common apt-transport-https apt-utils

RUN apt-get install -y build-essential zip bzip2 unzip tar wget make autoconf \
  automake libtool gcc libc++-dev libasound2-dev libcups2-dev libx11-dev \
  libjpeg-turbo8-dev libgif-dev libfreetype6-dev file libxtst-dev libxt-dev \
  libxrender-dev libfontconfig1-dev libgtk2.0-dev libatk-adaptor \
  git ant python3 mesa-common-dev libxkbcommon0 libgbm1

RUN add-apt-repository ppa:openjdk-r/ppa && apt-get update && apt-get install -y openjdk-11-jdk

RUN wget -O - https://apt.kitware.com/keys/kitware-archive-latest.asc 2>/dev/null | gpg --dearmor - >/usr/share/keyrings/kitware-archive-keyring.gpg && \
    echo 'deb [signed-by=/usr/share/keyrings/kitware-archive-keyring.gpg] https://apt.kitware.com/ubuntu/ xenial main' >/etc/apt/sources.list.d/kitware.list && \
    apt-get update && \
    apt-get install -y cmake
