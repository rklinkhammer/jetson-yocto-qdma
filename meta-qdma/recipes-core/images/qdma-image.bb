SUMMARY = "Minimal Jetson console image with Xilinx QDMA"
LICENSE = "MIT"
require recipes-core/images/core-image-minimal.bb
IMAGE_INSTALL:append = " qdma-tools kernel-module-qdma-pf"
# Development console login: root, no password. No SSH server is installed.
IMAGE_FEATURES += "allow-empty-password empty-root-password allow-root-login"
