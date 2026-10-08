SUMMARY = "Minimal Jetson console image with Xilinx QDMA"
LICENSE = "MIT"
require recipes-core/images/core-image-minimal.bb
# The module class makes qdma depend on its generated kernel-module packages.
# Request the recipe package so BitBake schedules the external driver build.
IMAGE_INSTALL:append = " qdma-tools qdma"
# Development console login: root, no password. No SSH server is installed.
IMAGE_FEATURES += "allow-empty-password empty-root-password allow-root-login"
