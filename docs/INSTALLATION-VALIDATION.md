# Installation validation

Hardware: Orin NX 16 GB P3767-0000 on P3509, Kingston 512 GB NVMe
(serial `50026B76874E13BE`), SanDisk USB (USB serial `5B4C0BC0E6DC`).

Final installed release: `qdma-image-p3509-a02-p3767-0000.rootfs-20261027033407.tegraflash-tar.zst`

Bundle SHA-256: `d4c410d93e7cd92b569a24c82fb2e0eebc1eec1bf09a73186021ce3fc7cfb69a`

USB payload raw SHA-256: `6d1a11b7092c553070f064474d6edfc5c7210ed4758de3cfebbfe33772e37d65`

USB payload gzip SHA-256: `e98be296a538398208b6d058af830faaab8f2725095c252ba46fde76a82bfe58`

- Release and installer builds: all 8,158 tasks passed.
- Initial clean recovery flash of QSPI and NVMe: SUCCESS, using release
  `20261027031101`. The final release adds UAS, efibootmgr and resize2fs; its UEFI
  binary is identical. The final NVMe installation uses the newer release above.
- Clean recovery boot: NVMe root, HDMI framebuffer, USB console, boot verifier,
  CUDA deviceQuery and vectorAdd passed; no failed systemd units.
- Installer USB boot: root on `/dev/sda1`, HDMI and USB console working;
  all target NVMe partitions unmounted, no failed systemd units.
- Negative checks: installer refused a mounted target and a wrong confirmation
  serial; neither test wrote storage.
- USB-to-NVMe installation: exit 0, compressed payload checksum and complete raw
  NVMe readback checksum matched; backup GPT relocated and APP filesystem grown.
- Installed filesystem: approximately 446 GiB usable, 418 GiB available after
  installation (ext4 metadata and reserved blocks account for the difference
  from the nominal 512 GB drive size).
- Receipt saved on NVMe as `/etc/qdma-install.json` and on USB as
  `/opt/qdma-installer/last-install.json`.
- Cold boot with the USB stick removed: passed; root is `/dev/nvme0n1p1`,
  no USB storage disk is present, and the installation receipt matches the release.
- Installed OS: HDMI framebuffer, USB serial console, boot verification and zero
  failed systemd units confirmed. PCI enumeration lists the NVMe controller.
- GPU development validation: CUDA deviceQuery/vectorAdd, native nvcc compilation
  and GPU kernel result, cuDNN 9.20 handle, TensorRT 10.16.2 builder, and trtexec
  help all passed.
- A second normal reboot without USB passed: NVMe-only storage, HDMI connected,
  USB root login, boot verification exit 0, no failed services, and both CUDA
  samples passed again. Boot IDs differed, confirming a real reboot.

Detailed logs are kept locally under `work/`; they are excluded from Git.

The USB installs the OS onto NVMe and can then be removed. It requires compatible
QSPI firmware; it is not a replacement for recovery flashing an unprovisioned or
unbootable module. The workflow has been tested on this one P3509/Orin NX setup,
not on other carriers, Secure Boot configurations or Orin Nano. QDMA hardware
transfers and AI inference accuracy are outside these checks.
