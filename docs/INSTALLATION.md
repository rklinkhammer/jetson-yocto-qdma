# Orin NX clean flash and USB installation

These instructions target the **Orin NX 16 GB (P3767-0000) on the NVIDIA P3509
carrier**, with an NVMe drive. Use the machine `p3509-a02-p3767-0000` throughout.
The image uses NVIDIA L4T R39.2.1 and a Yocto root filesystem, not Ubuntu. It
includes CUDA, cuDNN, TensorRT, native development tools, PCI utilities and QDMA.
HDMI is configured; the carrier's DisplayPort output is not enabled.

There are two installation methods:

* **Recovery flash from an x86-64 Linux host:** programs QSPI boot firmware and
  replaces the NVMe partition table and OS. Use this for initial provisioning or
  firmware recovery.
* **Bootable USB installer:** boots a separate Yocto maintenance system, replaces
  the NVMe partition table and OS, verifies readback, and expands the root
  filesystem. It does not rewrite QSPI. First provision compatible firmware using
  the recovery method. This is the project's Yocto installer, not NVIDIA's Ubuntu
  ISO installer, and it is distinct from running the installed OS from USB.

Both methods erase the destination NVMe installation. Copy needed files elsewhere
first. The USB creation command also erases the explicitly selected USB disk.
The installer supports this single-rootfs, non-secure-boot configuration only.
It does not provision fuses, encryption or Secure Boot keys.

## 1. Prepare the host and connections

1. Use an x86-64 Linux host with Docker Engine and the Compose plugin. Run the
   build as your normal account with Docker access, not with `sudo`.
2. Install host flashing and media tools. On Ubuntu:

   ```sh
   sudo apt update
   sudo apt install git python3 python3-serial zstd pigz cpio gdisk e2fsprogs \
       util-linux bmap-tools dosfstools udisks2 usbutils device-tree-compiler tio
   ```

   Docker provides the pinned Ubuntu 24.04 build environment. The tested host is
   Ubuntu 26.04 x86-64; it is not the NVIDIA SDK Manager supported-host claim.
3. Clone this repository and enter it:

   ```sh
   git clone https://github.com/rklinkhammer/jetson-yocto-qdma.git
   cd jetson-yocto-qdma
   ```

   If using a local checkout containing unpublished changes, use that checkout
   instead. Installer scripts must be present before following the USB steps.
4. Connect the Orin's Micro-USB device port to the host with a data cable and power
   it with its DC supply. Connect a monitor directly to **HDMI** if desired.
   The Micro-USB port supplies recovery transport, USB networking and the Linux
   serial console; it does not power the board.
5. Reserve sufficient host storage. The full CUDA build needs substantially more
   than a minimal console image. Keep at least 150 GB free for an initial build,
   and allow another 30 GB for release extraction and installer preparation.

## 2. Build and pin the release

```sh
MACHINE=p3509-a02-p3767-0000 ./yocto
MACHINE=p3509-a02-p3767-0000 ./yocto qdma-installer-image
```

Wait for each build to finish successfully. The second image is the maintenance
system used on the USB stick; `qdma-image` is the OS installed onto NVMe.

```sh
images="$PWD/build/p3509-a02-p3767-0000/tmp/deploy/images/p3509-a02-p3767-0000"
release=$(readlink -f "$images/qdma-image-p3509-a02-p3767-0000.rootfs.tegraflash-tar.zst")
installer=$(readlink -f "$images/qdma-installer-image-p3509-a02-p3767-0000.rootfs.tegraflash-tar.zst")
sha256sum "$release" "$installer"
```

Record the resolved filenames and checksums. Use the same `$release` for the
recovery flash and installer payload. Do not mix kernels, device trees, firmware
or modules from different builds. Build output symlinks move after later builds.

## 3. Reprogram from scratch in recovery mode

1. Leave the installer USB disconnected from the Orin. Close `tio` using Ctrl-T,
   then Q, and close other applications using the USB console.
2. Disconnect DC power. Bridge **J14 pins 9–10**, reconnect DC power, then remove
   the jumper. Keep Micro-USB connected to the host.
3. Confirm recovery detection:

   ```sh
   lsusb -d 0955:7323
   ```

   APX/recovery mode does not provide a normal `ttyACM` login console.
4. Extract into a new directory; the flashing tools modify their working files:

   ```sh
   mkdir -p work/clean-flash
   tar --zstd -xf "$release" -C work/clean-flash
   cd work/clean-flash
   sudo ./initrd-flash --erase-nvme
   cd ../..
   ```

   This includes boot firmware and NVMe. Do not use `--external-only` for initial
   provisioning. The script temporarily exposes the Jetson's NVMe to the host as
   a USB block device; do not manually write any host `/dev/sdX` during flashing.
5. Wait for `Final status: SUCCESS` and `Successfully finished`. Keep power and
   cables connected throughout. The script stores host and device logs alongside
   the extracted bundle. If it fails, retain the logs, correct the problem and
   enter recovery again before retrying.
6. Allow normal boot. If necessary, power off and on with the recovery jumper
   removed. Log in through HDMI or the USB console as `root`, with no password:

   ```sh
   tio /dev/ttyACM0
   ```

   The device number can change. Confirm the NVIDIA device before opening it.
7. Run the validation checks in section 7. The recovery writer makes an APP
   partition spanning the remaining NVMe, but the ext4 filesystem may retain its
   build-time size. On the current release, expand it online with
   `resize2fs /dev/nvme0n1p1`, then check `df -h /`. The USB installation below
   performs this expansion automatically.

## 4. Prepare the USB installer on the host

1. Build the checksummed whole-NVMe payload using the release's own partition
   layout and signed boot assets:

   ```sh
   scripts/prepare-installer-payload.sh "$release" work/installer-payload
   ```

   Use a new output directory. This creates a sparse raw image (at least 12 GB
   nominal, increased automatically for larger root filesystems) and
   compressed `nvme.img.gz`, with compressed, raw and bundle SHA-256 values in
   `release.json`. The script's offline signing step does not flash the Orin.
   The payload size includes room for boot partitions and filesystem growth.
2. Plug an erasable USB stick of at least 16 GB into the **host**, and identify its
   persistent USB ID:

   ```sh
   lsblk -o NAME,SIZE,MODEL,SERIAL,TRAN,MOUNTPOINTS
   ls -l /dev/disk/by-id/usb-*
   ```

   Choose the whole-disk link, not a `-partN` link. Unmount its mounted partitions.
3. Prepare the USB stick, substituting its actual ID:

   ```sh
   sudo scripts/create-installer-usb.sh "$installer" work/installer-payload \
       /dev/disk/by-id/usb-YOUR_DEVICE_ID work/installer-usb
   ```

   Read the displayed device details, then type `ERASE`. The script rejects
   mounted disks and non-USB targets. It writes the maintenance image, gives its
   root filesystem a unique UUID, patches its initrd to select only that USB
   filesystem, copies the release payload, verifies its checksum, and copies the
   repository with current working changes (excluding build/download/cache/work
   directories and `.env`) to `/opt/jetson-yocto-qdma` on the stick.
4. Wait for `Installer USB prepared and payload verified`. The script unmounts the
   USB root filesystem before returning. Move the stick to a USB-A port on the
   Orin. Keep Micro-USB attached to the host if using the serial console.

## 5. Boot the installer

1. With compatible boot firmware installed, connect the installer USB and an HDMI
   monitor/USB keyboard. Reboot and enter the UEFI boot manager with Esc during
   startup; select the USB device for this boot.
2. Alternatively, from the working Orin OS, inspect `efibootmgr -v` and select the
   entry explicitly describing the USB device with `efibootmgr -n XXXX`, then
   reboot. Never guess the entry number; it varies. If `efibootmgr` is absent,
   use the HDMI boot manager.
3. Log in as `root`, with no password. The banner should identify **QDMA YOCTO USB
   INSTALLER**. Verify the source before installing:

   ```sh
   findmnt /
   cat /etc/qdma-usb-installer
   cat /opt/qdma-installer/release.json
   qdma-install --dry-run
   ```

   `findmnt /` must point to the USB partition (typically `/dev/sda1`), not NVMe.
   The marker file is intentionally empty. A successful dry run validates the
   machine, USB root, unmounted NVMe and compressed payload without writing NVMe.

## 6. Install onto NVMe and remove the USB dependency

1. Run:

   ```sh
   qdma-install
   ```

2. Confirm the displayed NVMe serial and type the exact `ERASE <serial>` phrase.
   For an explicitly authorized scripted deployment, use
   `qdma-install --confirm-serial ACTUAL_NVME_SERIAL` instead.
3. Wait for `INSTALLATION PASSED`. The installer writes the complete partitioned
   image, verifies the raw bytes read back from NVMe, relocates the backup GPT,
   extends APP without changing its starting sector/GUID, checks and expands
   ext4, records `/etc/qdma-install.json` on NVMe, and selects an NVMe EFI entry.
   A receipt is also saved as `/opt/qdma-installer/last-install.json` on USB.
   Installation never runs automatically at boot.
4. Run `poweroff`. Once shut down, remove the USB installer and reconnect DC power
   with the recovery jumper removed. The system must now boot from NVMe without
   the stick. Retain the USB for future reinstallations.

## 7. Verify the installed system

From the Orin's root shell:

```sh
findmnt /
lsblk -o NAME,SIZE,FSTYPE,MOUNTPOINTS
df -h /
uname -r
systemctl --no-pager --failed
systemctl --no-pager status nv_update_verifier.service
nvbootctrl verify
cat /sys/class/drm/card*-HDMI-A-*/status
lspci -nn
/usr/bin/cuda-samples/deviceQuery
/usr/bin/cuda-samples/vectorAdd
nvcc --version
```

After USB installation also run:

```sh
cat /etc/qdma-install.json
```

Confirm the receipt identifies the expected release/serial and readback success;
root is `/dev/nvme0n1p1`; its filesystem fills the APP partition; HDMI displays the
console; the Micro-USB console permits login; boot verification passes; and both
CUDA samples pass. Copy this repository to the installed system and run
`sh scripts/validate-gpu.sh` to additionally compile a native CUDA/cuDNN/TensorRT
smoke test. Reboot once more **without the installer USB** and repeat root,
console, boot-verification and GPU checks to establish persistence.

`nv_update_verifier` is essential even with a single root filesystem. Without
successful boot verification, firmware can exhaust boot retries. CUDA compute
modules and NVIDIA display modules are separate; both are included in the current
release. Successful PCI enumeration does not validate QDMA transfers to hardware.

## References and validation record

NVIDIA documents its Ubuntu ISO installation and customization separately:
[flashing support](https://docs.nvidia.com/jetson/archives/r39.2.1/DeveloperGuide/SD/FlashingSupport.html)
and [ISO customization](https://docs.nvidia.com/jetson/archives/r39.2.1/DeveloperGuide/SD/JetsonISOCustomization.html).
This repository's installer uses the pinned meta-tegra release bundle and Yocto
maintenance image; it does not convert NVIDIA's Ubuntu autoinstall into Yocto.

See [installation validation](INSTALLATION-VALIDATION.md) for the actual hardware
checks, release identity and remaining limitations from the latest test.
