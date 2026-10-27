#!/bin/bash
# Prepare an explicitly selected USB disk. Requires a compatible flashed Orin.
set -euo pipefail
if [[ $# != 4 || $EUID != 0 ]]; then
    echo "Usage: sudo $0 INSTALLER_BUNDLE PAYLOAD_DIRECTORY /dev/disk/by-id/usb-ID NEW_WORK_DIRECTORY" >&2
    exit 2
fi
bundle=$(realpath "$1")
payload=$(realpath "$2")
device=$(realpath "$3")
work=$(realpath -m "$4")
[[ "$3" == /dev/disk/by-id/usb-* && -b "$device" ]] || { echo 'Select a USB whole disk by ID' >&2; exit 1; }
[[ $(lsblk -dn -o TYPE "$device") == disk && $(lsblk -dn -o TRAN "$device") == usb ]] || exit 1
[[ $(blockdev --getsize64 "$device") -ge 16000000000 ]] || { echo 'Use a USB disk of at least 16 GB' >&2; exit 1; }
[[ ! -e "$work" ]] || { echo 'Work directory already exists' >&2; exit 1; }
[[ -f "$payload/release.json" && -f "$payload/nvme.img.gz" ]] || exit 1
if lsblk -nr -o MOUNTPOINTS "$device" | grep -q '[^[:space:]]'; then
    echo 'Unmount all partitions on the USB disk first' >&2
    exit 1
fi
lsblk -o NAME,SIZE,MODEL,SERIAL,TRAN "$device"
read -r -p "Erase $3? Type ERASE: " answer
[[ "$answer" == ERASE ]] || exit 1
mkdir -p "$work/bundle" "$work/root" "$work/initrd"
mounted=no
cleanup() { [[ $mounted == no ]] || umount "$work/root"; }
trap cleanup EXIT
tar --zstd -xf "$bundle" -C "$work/bundle"
grep -qx 'MACHINE="p3509-a02-p3767-0000"' "$work/bundle/.env.initrd-flash"
(
    cd "$work/bundle"
    ./doexternal.sh -y --keep-connection "$device"
) > "$work/write.log" 2>&1
udevadm settle
partition="${device}1"
uuid=$(cat /proc/sys/kernel/random/uuid)
tune2fs -U "$uuid" -L QDMA-INSTALL "$partition"
resize2fs "$partition"
mount "$partition" "$work/root"
mounted=yes
root="$work/root"
touch "$root/etc/qdma-usb-installer"
test -x "$root/usr/sbin/qdma-install"
mkdir -p "$root/opt/qdma-installer"
cp "$payload/nvme.img.gz" "$payload/release.json" "$root/opt/qdma-installer/"
# Keep the source repository and current working changes available offline.
repo=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$root/opt/jetson-yocto-qdma"
tar -C "$repo" --exclude='./build' --exclude='./downloads' --exclude='./sstate-cache' \
    --exclude='./work' --exclude='./.env' --exclude='__pycache__' -cf - . \
    | tar -C "$root/opt/jetson-yocto-qdma" -xf -
# The stock initrd selects NVMe. Bind this USB initrd to its unique filesystem
# UUID so a failed USB boot cannot silently run the installer on the NVMe root.
gzip -dc "$root/boot/initrd" | (cd "$work/initrd" && cpio -idm --quiet)
cp -a "$root/usr/lib/modules/." "$work/initrd/usr/lib/modules/"
for modules in "$work/initrd/usr/lib/modules/"*; do
    depmod -b "$work/initrd" "$(basename "$modules")"
done
cat > "$work/initrd/etc/platform-preboot" <<EOF
modprobe usb-storage 2>/dev/null || true
modprobe uas 2>/dev/null || true
for count in \$(seq 1 120); do
    rootdev=\$(blkid -t UUID=$uuid -l | cut -d: -f1)
    [ -z "\$rootdev" ] || break
    sleep 0.5
done
if [ -z "\$rootdev" ]; then
    echo 'Installer USB not found; refusing to boot another root filesystem'
    exec sh
fi
opt=rw
EOF
(cd "$work/initrd" && find . -print0 | cpio --null -o -H newc --quiet | gzip -1) > "$root/boot/initrd"
cat > "$root/etc/motd" <<'EOF'
QDMA YOCTO USB INSTALLER
Log in as root (no password). No disk is erased automatically.
Run: qdma-install --dry-run
Then: qdma-install
After success: poweroff, remove the USB stick, power on normally.
EOF
python3 - "$root/opt/qdma-installer" <<'PY'
import hashlib,json,pathlib,sys
p=pathlib.Path(sys.argv[1]); m=json.loads((p/'release.json').read_text())
with (p/'nvme.img.gz').open('rb') as f:
    assert hashlib.file_digest(f,'sha256').hexdigest()==m['gzip_sha256'], 'USB payload copy is corrupt'
PY
sync
umount "$root"
mounted=no
echo "Installer USB prepared and payload verified. UUID: $uuid"
