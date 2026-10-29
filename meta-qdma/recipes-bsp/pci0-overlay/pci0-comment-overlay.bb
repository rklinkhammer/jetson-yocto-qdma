SUMMARY = "PCI0 host bridge clock-request overlay"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
FILESEXTRAPATHS:prepend := "${THISDIR}/../../../boot-config:"
SRC_URI = "file://pci0-comment-overlay.dts file://pcie-c0"
S = "${UNPACKDIR}"
DEPENDS = "dtc-native"
COMPATIBLE_MACHINE = "^p3509-a02-p3767-0000$"
PACKAGE_ARCH = "${MACHINE_ARCH}"
do_compile() {
    dtc -@ -I dts -O dtb -o ${B}/pci0-comment-overlay.dtbo ${S}/pci0-comment-overlay.dts
}
do_install() {
    install -d ${D}${datadir}/qdma/device-tree
    install -m 0644 ${S}/pci0-comment-overlay.dts ${B}/pci0-comment-overlay.dtbo ${D}${datadir}/qdma/device-tree/
    # Keep the reviewed boot change available for explicit manual installation.
    cp -R --no-preserve=ownership ${S}/pcie-c0 ${D}${datadir}/qdma/device-tree/
}
FILES:${PN} = "${datadir}/qdma/device-tree"
