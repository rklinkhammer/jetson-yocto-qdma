SUMMARY = "Xfce desktop account, network ownership and SSH defaults"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
inherit useradd
USERADD_PACKAGES = "${PN}"
USERADD_DEPENDS = "systemd tegra-configs"
GROUPADD_PARAM:${PN} = "--gid 1000 jetson"
USERADD_PARAM:${PN} = "--uid 1000 --gid jetson --create-home --shell /bin/bash --groups audio,video,input,render,wheel,debug --password '' jetson"
RDEPENDS:${PN} = "bash lxdm sudo openssh-sshd networkmanager tegra-configs-udev"

do_install() {
    install -d ${D}${sysconfdir}/ssh/sshd_config.d ${D}${sysconfdir}/sudoers.d
    cat > ${D}${sysconfdir}/ssh/sshd_config.d/00-qdma-auth.conf <<'EOF'
# Local development consoles allow empty passwords; remote logins never do.
PermitEmptyPasswords no
PermitRootLogin prohibit-password
PubkeyAuthentication yes
PasswordAuthentication yes
KbdInteractiveAuthentication no
EOF
    echo '%wheel ALL=(ALL:ALL) ALL' > ${D}${sysconfdir}/sudoers.d/10-qdma-wheel
    chmod 0440 ${D}${sysconfdir}/sudoers.d/10-qdma-wheel
    install -d ${D}${sysconfdir}/NetworkManager/conf.d ${D}${sysconfdir}/systemd/network
    cat > ${D}${sysconfdir}/NetworkManager/conf.d/10-qdma.conf <<'EOF'
[main]
dns=systemd-resolved
[keyfile]
unmanaged-devices=interface-name:usb0;interface-name:usb1;interface-name:l4tbr0
EOF
    # NetworkManager owns physical Ethernet/Wi-Fi; preserve the USB gadget setup.
    ln -s /dev/null ${D}${sysconfdir}/systemd/network/80-wired.network
}
FILES:${PN} += "${sysconfdir}"
CONFFILES:${PN} = "${sysconfdir}/ssh/sshd_config.d/00-qdma-auth.conf ${sysconfdir}/sudoers.d/10-qdma-wheel ${sysconfdir}/NetworkManager/conf.d/10-qdma.conf"
