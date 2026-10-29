SUMMARY = "Patched QDMA utility and kernel driver sources for native development"
LICENSE = "GPL-2.0-only & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://COPYING;md5=eb723b61539feef013de476e68b5c50a file://LICENSE;md5=4969aeb8afed4d3b53cd8fdefc61ad40"
FILESEXTRAPATHS:prepend := "${THISDIR}/../qdma-tools/files:${THISDIR}/../../recipes-kernel/qdma/files:"
SRC_URI = "git://github.com/Xilinx/dma_ip_drivers.git;protocol=https;nobranch=1 \
    file://0001-honor-linker-flags.patch;patchdir=../.. \
    file://0002-supply-create-mode.patch;patchdir=../.. \
    file://0001-use-monotonic-nanoseconds.patch;patchdir=../.. \
    file://0002-use-kbuild-M-for-cross-compilation.patch;patchdir=../.. \
    file://0003-preserve-recursive-compiler-overrides.patch;patchdir=../.. \
    file://README.md file://build-tools.sh \
"
SRCREV = "b8466090b4e812e191da9e9305ffb11cb7ace768"
S = "${UNPACKDIR}/${BP}/QDMA/linux-kernel"
do_configure[noexec] = "1"
do_compile[noexec] = "1"
do_install() {
    install -d ${D}${prefix}/src/qdma/linux-kernel ${D}${prefix}/src/qdma/patches
    cp -R --no-preserve=ownership ${S}/. ${D}${prefix}/src/qdma/linux-kernel/
    install -m 0644 ${UNPACKDIR}/README.md ${D}${prefix}/src/qdma/
    install -m 0755 ${UNPACKDIR}/build-tools.sh ${D}${prefix}/src/qdma/
    install -m 0644 ${UNPACKDIR}/*.patch ${D}${prefix}/src/qdma/patches/
    echo '${SRCREV}' > ${D}${prefix}/src/qdma/UPSTREAM_REVISION
}
FILES:${PN} = "${prefix}/src/qdma"
RDEPENDS:${PN} = "bash make gcc binutils"
