do_install:append() {
    sed -i -e 's|^# autologin=.*|autologin=jetson|' \
           -e 's|^# session=.*|session=${bindir}/startxfce4|' \
           ${D}${sysconfdir}/lxdm/lxdm.conf
}
