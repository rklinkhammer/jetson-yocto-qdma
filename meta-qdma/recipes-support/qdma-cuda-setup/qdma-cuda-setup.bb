SUMMARY = "Headless Orin CUDA module loading and development shell setup"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://qdma-cuda.conf file://cuda.sh"
S = "${UNPACKDIR}"
COMPATIBLE_MACHINE = "tegra234"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# libcuda needs the host1x DRM render node even without a display server.
RDEPENDS:${PN} = "nv-kernel-module-nvgpu nv-kernel-module-tegra-drm"

do_install() {
    install -D -m 0644 ${S}/qdma-cuda.conf ${D}${nonarch_libdir}/modules-load.d/qdma-cuda.conf
    install -D -m 0644 ${S}/cuda.sh ${D}${sysconfdir}/profile.d/cuda.sh
}

FILES:${PN} += "${nonarch_libdir}/modules-load.d"
