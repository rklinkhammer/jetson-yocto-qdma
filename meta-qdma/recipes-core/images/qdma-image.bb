SUMMARY = "Jetson CUDA development console image with Xilinx QDMA"
LICENSE = "MIT"
require recipes-core/images/core-image-minimal.bb
# The module class makes qdma depend on its generated kernel-module packages.
# Request the recipe package so BitBake schedules the external driver build.
IMAGE_INSTALL:append = " qdma-tools qdma l4t-usb-device-mode tegra-redundant-boot pciutils pciutils-ids pcimem"
# Include the GPU driver explicitly as well as the CUDA runtime and compiler.
IMAGE_INSTALL:append = " \
    tegra-libraries-cuda qdma-cuda-setup \
    cuda-toolkit cuda-target-environment cuda-compatibility-workarounds-dev \
    cuda-samples cudnn cudnn-dev tensorrt tensorrt-dev tensorrt-headers-dev \
    tensorrt-plugins-prebuilt tensorrt-plugins-prebuilt-dev tensorrt-trtexec-prebuilt \
    cmake ninja git gdb gdbserver strace ltrace pkgconfig \
"
# Native C/C++ compiler, libc headers, make and related development tools.
IMAGE_FEATURES += "tools-sdk"
# l4t-usb-device-mode exposes a USB ACM console and USB networking.
# Development console login: root, no password. No SSH server is installed.
IMAGE_FEATURES += "allow-empty-password empty-root-password allow-root-login"

IMAGE_INSTALL:append:p3509-a02-p3767-0000 = " p3509-usb-device-role pci0-comment-overlay"
