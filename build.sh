#!/bin/bash
set -eo pipefail
export MACHINE=${MACHINE:-p3768-0000-p3767-0000}
[[ $MACHINE =~ ^[a-zA-Z0-9_-]+$ && -f /opt/yocto/repos/meta-tegra/conf/machine/$MACHINE.conf ]] || {
    echo "Unknown meta-tegra machine: $MACHINE" >&2; exit 2;
}
build=/project/build/$MACHINE
mkdir -p "$build/conf"
exec 9>"$build/.lock"
flock -n 9 || { echo "A build for $MACHINE is already running." >&2; exit 1; }
cat > "$build/conf/bblayers.conf" <<'CONF'
LCONF_VERSION = "7"
BBPATH = "${TOPDIR}"
BBFILES ?= ""
BBLAYERS = "/opt/yocto/repos/openembedded-core/meta /opt/yocto/repos/meta-openembedded/meta-oe /opt/yocto/repos/meta-openembedded/meta-python /opt/yocto/repos/meta-openembedded/meta-networking /opt/yocto/repos/meta-openembedded/meta-multimedia /opt/yocto/repos/meta-openembedded/meta-gnome /opt/yocto/repos/meta-openembedded/meta-xfce /opt/yocto/repos/meta-tegra /project/meta-qdma"
CONF
printf 'MACHINE = "%s"\nrequire /project/local.conf\n' "$MACHINE" > "$build/conf/local.conf"
export BITBAKEDIR=/opt/yocto/repos/bitbake
source /opt/yocto/repos/openembedded-core/oe-init-build-env "$build"
bitbake "${@:-qdma-image}"
