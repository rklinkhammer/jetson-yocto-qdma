SUMMARY = "Minimal Jetson console image with Xilinx QDMA"
LICENSE = "MIT"
require recipes-core/images/core-image-minimal.bb
# The module class makes qdma depend on its generated kernel-module packages.
# Request the recipe package so BitBake schedules the external driver build.
IMAGE_INSTALL:append = " qdma-tools qdma l4t-usb-device-mode pciutils pciutils-ids pcimem"
# l4t-usb-device-mode exposes a USB ACM console and USB networking.
# Development console login: root, no password. No SSH server is installed.
IMAGE_FEATURES += "allow-empty-password empty-root-password allow-root-login"

IMAGE_INSTALL:append:p3509-a02-p3767-0000 = " p3509-usb-device-role pci0-comment-overlay"
