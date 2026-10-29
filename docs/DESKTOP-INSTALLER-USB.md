# Full desktop USB with installation tools

This variant boots the actual released Xfce/CUDA/DTC/QDMA development filesystem
from USB. It also includes `qdma-install` and the separately checksummed release
payload in `/opt/qdma-installer`. The repository is in `/opt/jetson-yocto-qdma`.

The USB root is writable: experiments persist across reboots. It starts from the
same released filesystem installed onto NVMe, with USB-specific changes: root
UUID/initrd selection, expanded storage, added installer packages, installer
marker/banner, and a udev rule hiding NVMe from desktop automounting. It is not a
byte-identical NVMe root copy after these changes.

## Boot and experiment

Use a matching Orin NX 16 GB/P3509 with compatible QSPI boot firmware. Select the
USB entry in the UEFI boot menu. Xfce starts as `jetson`; the local root console
remains available without a password. SSH rejects empty passwords.

Verify before experimenting:

```sh
findmnt /
lsblk -o NAME,SIZE,LABEL,MOUNTPOINTS
dtc --version
ls /usr/src/qdma
```

Root must be the USB partition (usually `/dev/sda1`, label `QDMA-DESKTOP`), not
NVMe. The initrd selects the unique USB filesystem UUID and stops rather than
falling back to NVMe if it cannot find it. NVMe is hidden from the desktop's
volume manager but remains accessible to deliberate root commands.

## Install the original release onto NVMe

Close applications using NVMe and ensure no NVMe partitions are mounted. Then:

```sh
sudo qdma-install --dry-run
sudo qdma-install
```

If the desktop user has no password yet, use the local root console instead.
The first command writes nothing. The second erases NVMe only after explicit
serial confirmation, checks readback and expands the installed filesystem.
After success, power off, remove the USB and power on normally.

**Installation uses the saved original payload, not your modified USB root.**
Experiments and local files on USB are not transferred into the installation.
Do not edit `/opt/qdma-installer/nvme.img.gz` or `release.json`.

## Recreate from the host

Use a USB drive of at least 32 GB. The script requires the same host tools as the
maintenance installer, plus working ARM64 binfmt/QEMU support for installing the
matching IPK packages into the offline USB root. Build outputs and package feeds
from the same release must remain available.

```sh
sudo scripts/create-desktop-installer-usb.sh RELEASE.tegraflash-tar.zst \
    PAYLOAD_DIRECTORY /dev/disk/by-id/usb-YOUR_DEVICE_ID NEW_WORK_DIRECTORY
```

The selected USB is erased after confirmation. The script verifies the input
payload, writes it to USB, expands the root, assigns unique disk/partition/root
identities, adds the installer packages, fixes USB root selection and verifies
the copied payload. The regular `create-installer-usb.sh` continues to create the
smaller maintenance-only environment.
