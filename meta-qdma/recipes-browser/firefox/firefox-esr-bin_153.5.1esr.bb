SUMMARY = "Mozilla Firefox ESR official ARM64 browser"
HOMEPAGE = "https://www.firefox.com/"
LICENSE = "MPL-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MPL-2.0;md5=815ca599c9df247a0c7f619bab123dad"
# The upstream archive retains third-party notices in about:license (omni.ja).
SRC_URI = "https://archive.mozilla.org/pub/firefox/releases/${PV}/linux-aarch64/en-US/firefox-${PV}.tar.xz"
SRC_URI[sha256sum] = "e566ecb209917c0b4d11813212c5afae4383f28ca554c4ad651df044cb255cde"
S = "${UNPACKDIR}/firefox"
COMPATIBLE_HOST = "aarch64.*-linux"

inherit update-alternatives mime-xdg gtk-icon-cache
DEPENDS += "gtk+3 dbus alsa-lib libx11 libxcomposite libxdamage libxext libxfixes libxrandr libxrender libxcursor libxi libxcb fontconfig freetype"
RDEPENDS:${PN} += "gtk+3 dbus alsa-lib libx11 libxcomposite libxdamage libxext libxfixes libxrandr libxrender libxcursor libxi libxcb fontconfig freetype libstdc++ libgcc glibc"
# Mozilla's private copies must not become providers for other system packages.
PRIVATE_LIBS = "libfreeblpriv3.so libgkcodecs.so liblgpllibs.so libmozavcodec.so libmozavutil.so libmozgtk.so libmozinference.so libmozsandbox.so libmozsqlite3.so libmozwayland.so libnspr4.so libnss3.so libnssutil3.so libplc4.so libplds4.so libsmime3.so libsoftokn3.so libssl3.so libxul.so"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INSANE_SKIP:${PN} = "already-stripped ldflags"

do_configure[noexec] = "1"
do_compile[noexec] = "1"
do_install() {
    install -d ${D}/opt/firefox ${D}${bindir} ${D}${datadir}/applications
    cp -R --no-preserve=ownership ${S}/. ${D}/opt/firefox/
    ln -s /opt/firefox/firefox ${D}${bindir}/firefox
    for size in 16 32 48 64 128; do
        install -d ${D}${datadir}/icons/hicolor/${size}x${size}/apps
        install -m 0644 ${S}/browser/chrome/icons/default/default${size}.png ${D}${datadir}/icons/hicolor/${size}x${size}/apps/firefox.png
    done
    cat > ${D}${datadir}/applications/firefox.desktop <<'EOF'
[Desktop Entry]
Type=Application
Name=Firefox ESR
Comment=Web Browser
Exec=firefox %u
Icon=firefox
Terminal=false
Categories=Network;WebBrowser;
MimeType=text/html;application/xhtml+xml;x-scheme-handler/http;x-scheme-handler/https;
StartupNotify=true
EOF
    install -d ${D}/opt/firefox/distribution
    echo '{"policies":{"DisableAppUpdate":true}}' > ${D}/opt/firefox/distribution/policies.json
}
FILES:${PN} = "/opt/firefox ${bindir}/firefox ${datadir}/applications/firefox.desktop ${datadir}/icons"
ALTERNATIVE:${PN} = "x-www-browser"
ALTERNATIVE_LINK_NAME[x-www-browser] = "${bindir}/x-www-browser"
ALTERNATIVE_TARGET[x-www-browser] = "${bindir}/firefox"
ALTERNATIVE_PRIORITY[x-www-browser] = "100"
