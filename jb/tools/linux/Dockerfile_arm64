FROM ubuntu:bionic

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update &&  \
    apt-get install -y software-properties-common apt-transport-https apt-utils \
    build-essential zip bzip2 unzip tar wget make autoconf \
    automake libtool gcc libc++-dev libasound2-dev libcups2-dev libx11-dev \
    libjpeg-turbo8-dev libgif-dev libfreetype6-dev file libxtst-dev libxt-dev \
    libxrender-dev libfontconfig1-dev libgtk2.0-dev libatk-adaptor \
    git ant python3 mesa-common-dev libxkbcommon0 libgbm1 curl pkg-config bison flex

RUN wget -nv -O jdk.deb https://cache-redirector.jetbrains.com/corretto.aws/downloads/resources/17.0.3.6.1/java-17-amazon-corretto-jdk_17.0.3.6-1_$(dpkg-architecture -q DEB_BUILD_ARCH).deb && \
    dpkg -i jdk.deb
ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
RUN ${JAVA_HOME}/bin/javac -version

ARG ANT_VERSION=1.10.14
RUN wget -nv -O ant.tgz https://dlcdn.apache.org//ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar xfz ant.tgz -C /opt && \
    rm ant.tgz
ENV ANT_HOME=/opt/apache-ant-${ANT_VERSION}
RUN ${ANT_HOME}/bin/ant -version

RUN wget -nv -O /cmake.tar.gz https://github.com/Kitware/CMake/releases/download/v3.23.1/cmake-3.23.1-linux-$(uname -m).tar.gz && \
    tar xfz /cmake.tar.gz --strip-components=1 -C /usr/local && \
    rm /cmake.tar.gz && \
    cmake --version

RUN git config --global --add safe.directory '*'

#RUN git clone https://github.com/JetBrains/jcef.git && cd jcef && git checkout dev
#RUN echo "env BUILD_CEF_SERVER=1 bash ./jb/tools/linux/build.sh all x86_64" > run.sh
