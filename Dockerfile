FROM ubuntu:24.04@sha256:534baea6a22c03a63003dbc8dbe78fe34bc0d7e595d9a9dc9834884ff530eb55
ENV DEBIAN_FRONTEND=noninteractive LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential chrpath cpio debianutils diffstat file gawk gcc git git-lfs \
    iputils-ping libacl1 liblz4-tool locales python3 python3-git python3-jinja2 \
    python3-pexpect python3-pip python3-subunit socat texinfo unzip wget xz-utils \
    zstd ca-certificates bison flex libssl-dev libelf-dev bc rsync curl \
    util-linux acl \
    && locale-gen en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*
# Ubuntu's existing non-root user is UID/GID 1000.
RUN mkdir /opt/yocto && chown ubuntu:ubuntu /opt/yocto
USER ubuntu
# One manifest pins all four upstream repositories to compatible revisions.
ARG YOCTO_REV=6eab4529e8ba83231214677dc70f504ca1cb326f
RUN git -C /opt/yocto init \
    && git -C /opt/yocto remote add origin https://github.com/OE4T/tegra-demo-distro.git \
    && git -C /opt/yocto fetch --depth 1 origin ${YOCTO_REV} \
    && git -C /opt/yocto checkout --detach FETCH_HEAD \
    && git -C /opt/yocto submodule update --init --depth 1 --jobs 4 \
       repos/bitbake repos/openembedded-core repos/meta-openembedded repos/meta-tegra
WORKDIR /project
ENTRYPOINT ["/bin/bash", "/project/build.sh"]
CMD ["qdma-image"]
