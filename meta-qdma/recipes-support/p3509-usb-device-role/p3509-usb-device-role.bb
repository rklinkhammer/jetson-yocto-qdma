SUMMARY = "USB device-role fallback for the P3509 console port"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
SRC_URI = "file://p3509-usb-device-role file://p3509-device-role.conf"
S = "${UNPACKDIR}"
COMPATIBLE_MACHINE = "^p3509-a02-p3767-0000$"
PACKAGE_ARCH = "${MACHINE_ARCH}"
RDEPENDS:${PN} = "l4t-usb-device-mode"

do_install() {
    install -d ${D}${sbindir} ${D}${sysconfdir}/systemd/system/usbgx.service.d
    install -m 0755 ${S}/p3509-usb-device-role ${D}${sbindir}/
    install -m 0644 ${S}/p3509-device-role.conf ${D}${sysconfdir}/systemd/system/usbgx.service.d/
}
