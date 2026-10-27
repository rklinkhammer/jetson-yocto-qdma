SUMMARY = "USB maintenance system for installing the QDMA Yocto release to NVMe"
LICENSE = "MIT"
require recipes-core/images/core-image-minimal.bb

COMPATIBLE_MACHINE = "p3509-a02-p3767-0000"
IMAGE_FEATURES += "allow-empty-password empty-root-password allow-root-login"
IMAGE_INSTALL:append = " \
    qdma-usb-installer l4t-usb-device-mode p3509-usb-device-role \
    tegra-redundant-boot tegra-firmware nvidia-kernel-oot-display \
    nv-kernel-module-nvgpu tegra-configs-display-driver kernel-module-uas \
"
