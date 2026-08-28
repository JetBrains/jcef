ARG BASE_IMAGE=registry.jetbrains.team/p/jbre/containers/oraclelinux8_x64:latest
FROM ${BASE_IMAGE}

# The ol8 repositories provide bison-3.0.4 only, while the build requires bison-3.8.2+
ARG BISON_VERSION=3.8.2
ARG BISON_URL=https://mirrors.kernel.org/gnu/bison/bison-${BISON_VERSION}.tar.gz
ARG BISON_SHA256=06c9e13bdf7eb24d4ceb6b59205a4f67c2c7e7213119644430fe82fbd14a0abb
RUN yum -y install m4 make wget perl-Time-Piece && \
    yum -y clean all && \
    wget -nv -O bison.tar.gz ${BISON_URL} && \
    echo "${BISON_SHA256}  bison.tar.gz" | sha256sum -c - && \
    tar xfz bison.tar.gz && \
    cd bison-${BISON_VERSION} && \
    ./configure --prefix=/usr/local && \
    make -j$(nproc) && \
    make install && \
    cd .. && \
    rm -rf bison.tar.gz bison-${BISON_VERSION}

ENV PATH=/usr/local/bin:${PATH}
RUN bison --version
