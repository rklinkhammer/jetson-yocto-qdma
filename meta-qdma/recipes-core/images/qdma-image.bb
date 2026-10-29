SUMMARY = "Jetson Xfce and CUDA development image with Xilinx QDMA"
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
# Device-tree compiler, FDT utilities, comparison/conversion helpers and C headers.
IMAGE_INSTALL:append = " dtc dtc-misc dtc-dev"
# Native rebuilds of the packaged QDMA tools and external kernel module.
IMAGE_INSTALL:append = " qdma-sources libaio-dev kernel-devsrc elfutils-dev patch perl"
# Native C/C++ compiler, libc headers, make and related development tools.
IMAGE_FEATURES += "tools-sdk"
IMAGE_INSTALL:append = " packagegroup-core-x11 packagegroup-xfce-base dbus liberation-fonts"
IMAGE_INSTALL:append = " \
    qdma-desktop-setup xfce4-appfinder xfce4-power-manager xfce-polkit mousepad \
    xarchiver thunar-archive-plugin gvfs gvfsd-sftp gvfsd-trash udisks2 \
    networkmanager network-manager-applet linux-firmware wireless-regdb kernel-modules \
    pulseaudio-server pulseaudio-module-alsa-card alsa-utils pavucontrol \
    xfce4-pulseaudio-plugin bluez5 blueman firefox-esr-bin \
    sudo openssh-sftp rsync curl wget ca-certificates nano vim tmux htop \
    usbutils ethtool iproute2 iproute2-ss iputils bind-utils tcpdump nftables \
    tar gzip bzip2 xz zip unzip less file procps psmisc util-linux \
    python3 python3-pip \
"
IMAGE_FEATURES += "ssh-server-openssh package-management"
SYSTEMD_DEFAULT_TARGET = "graphical.target"
# l4t-usb-device-mode exposes a USB ACM console and USB networking.
# Development console login: root, no password. SSH disallows empty passwords.
IMAGE_FEATURES += "allow-empty-password empty-root-password allow-root-login"

IMAGE_INSTALL:append:p3509-a02-p3767-0000 = " p3509-usb-device-role pci0-comment-overlay"
# The compute GPU modules do not provide HDMI scanout. Include the display
# modules and upstream module-loading configuration for the P3509 HDMI output.
IMAGE_INSTALL:append:p3509-a02-p3767-0000 = " nvidia-kernel-oot-display tegra-configs-display-driver"
# USB installer media and maintenance of the installed NVMe filesystem.
IMAGE_INSTALL:append:p3509-a02-p3767-0000 = " kernel-module-uas efibootmgr e2fsprogs-resize2fs"

# NVIDIA carries these Realtek drivers outside the main kernel module package.
IMAGE_INSTALL:append:p3509-a02-p3767-0000 = " nv-kernel-module-r8168 nv-kernel-module-rtl8822ce nv-kernel-module-rtk-btusb tegra-firmware-rtl8822"
