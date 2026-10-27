SUMMARY = "Interactive USB-to-NVMe Yocto installer"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://qdma-install"
S = "${UNPACKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
FILES:${PN} += "${datadir}/qdma-installer"
RDEPENDS:${PN} = "python3 gptfdisk util-linux util-linux-lsblk util-linux-findmnt util-linux-blockdev e2fsprogs-e2fsck e2fsprogs-resize2fs efibootmgr coreutils"
do_install() {
    install -Dm0755 ${S}/qdma-install ${D}${sbindir}/qdma-install
    install -d ${D}${datadir}/qdma-installer
    echo '${MACHINE}' > ${D}${datadir}/qdma-installer/machine
}
