# Xfce desktop and remote administration

The desktop release extends the Yocto CUDA/QDMA image with Xfce and common
workstation/server utilities. It remains a Yocto distribution, not Ubuntu; its
package format is IPK, and Ubuntu APT repositories and `.deb` packages are not
compatible with it.

## Included software

- Xfce panel, window manager, terminal, Thunar file manager, settings, power
  manager and PolicyKit authentication agent; LXDM starts the graphical session.
- Firefox ESR (Mozilla’s official ARM64 binary), Mousepad, Xarchiver and Thunar archive integration.
- NetworkManager with its graphical applet, Ethernet/Wi-Fi support, Bluetooth
  tools, PulseAudio/ALSA controls and removable-drive management.
- OpenSSH server/client and SFTP, sudo, rsync, curl, wget, certificates, nano,
  Vim, tmux, htop, USB/PCI diagnostics, ethtool, iproute2, DNS utilities, tcpdump,
  nftables, archive tools, Python and pip.
- The existing CUDA toolkit, cuDNN, TensorRT, native compilers and QDMA tools.

Installing a network, audio or Bluetooth package does not establish that every
adapter or peripheral works. Hardware validation is recorded separately.

## Desktop startup

Use the P3509 **HDMI** output and attach a USB keyboard/mouse. The image boots to
`graphical.target`, and LXDM automatically starts Xfce as the local `jetson` user.
The local development account initially has no password. Root login through the
Micro-USB console remains available.

To inspect or restart the desktop from the root USB console:

```sh
systemctl status lxdm --no-pager
journalctl -b -u lxdm --no-pager
systemctl restart lxdm
```

Restarting LXDM closes the current desktop session. To inspect display detection:

```sh
cat /sys/class/drm/card*-HDMI-A-*/status
```

## Set up SSH when ready

OpenSSH is installed and listens on port 22. **Empty-password SSH login is
rejected**, including for root. No user keys or passwords are embedded in the
release. Root SSH permits keys only.

For password login, open the local root console and set the desktop account's
password interactively:

```sh
passwd jetson
```

From the connected Linux host (with USB networking configured):

```sh
ssh jetson@192.168.55.1
```

For LAN access, substitute the Orin's address from `ip -br addr` or the network
applet. The `jetson` user is in `wheel` and can use `sudo`, authenticating with its
own password. To use a key instead, copy only your **public** key to
`/home/jetson/.ssh/authorized_keys`, set directory mode 700 and file mode 600,
and make both owned by `jetson:jetson`.

The defaults are in `/etc/ssh/sshd_config.d/00-qdma-auth.conf`. Check effective
settings with:

```sh
sshd -T | grep -E 'permitrootlogin|permitemptypasswords|passwordauthentication'
systemctl status sshd.socket sshd.service --no-pager
```

SSH may use socket activation, so a listening `sshd.socket` with an inactive
standalone `sshd.service` can be normal. SSH host keys are generated on the
installed device, not copied from another machine.

## Networking

NetworkManager owns physical Ethernet and Wi-Fi. Use the panel applet or:

```sh
nmcli device
nmcli --ask device wifi connect 'YOUR_SSID'
```

The existing USB gadget interfaces (`usb0`, `usb1`, `l4tbr0`) remain unmanaged by
NetworkManager. The USB console and its 192.168.55.1 network remain independent of
the desktop's LAN configuration. The generic systemd-networkd Ethernet DHCP
profile is masked to avoid two managers configuring the same physical interface.

## Building and installing

Firefox is packaged from Mozilla's official ARM64 ESR archive with a pinned
version and SHA-256 checksum. Its built-in updater is disabled so browser updates
remain part of the Yocto release: update the recipe version/checksum and rebuild.
The browser's upstream third-party notices remain available in `about:license`.
See [Mozilla's deployment documentation](https://firefox-admin-docs.mozilla.org/guides/getting-started/).

Build the release with:

```sh
MACHINE=p3509-a02-p3767-0000 ./yocto
```

Follow [the installation guide](INSTALLATION.md) for a recovery flash or USB
installer. A previously created installer stick still contains its old payload;
rebuilding the image alone does not update that stick.

`opkg` and its installed-package database are included. No public package feed is
configured by this project. Additional packages should be built from this same
pinned Yocto configuration, or added to the image and rebuilt. `pip` is available
for Python environments; it does not replace the OS package build process.

## Validation

Validated on 2026-10-27 on Orin NX 16 GB/P3509. All 13,101 BitBake tasks
passed. The flashable archive in the machine's deploy directory is:

```text
qdma-image-p3509-a02-p3767-0000.rootfs-20261027064939.tegraflash-tar.zst
SHA256 c9cc84f8f287b35cdcda733afc5de740db77c41de1b5e7c4a27172fc1c6b5103
```

The running NVMe installation was updated online, preserving existing accounts,
password hashes, machine identity and SSH host keys. A normal reboot verified:

- Xfce autologin as `jetson`, HDMI connected at 1920×1080, and Firefox GUI and
  headless page rendering (rendering checked before the final reboot).
- OpenSSH listening with empty passwords rejected and root restricted to keys;
  user credentials are intentionally not configured yet.
- Ethernet DHCP and HTTPS connectivity, Wi-Fi adapter detection/scanning, and
  Bluetooth controller detection. Wi-Fi association, Bluetooth pairing and
  actual audio playback have not been tested. PulseAudio starts for the user.
- Native CUDA compilation, `deviceQuery`, `vectorAdd`, a GPU kernel, cuDNN handle
  creation and TensorRT builder creation all pass as the ordinary `jetson` user.
- Successful NVIDIA boot verification, synchronized time and no failed systemd
  services.

The image explicitly includes NVIDIA's Realtek Ethernet/Wi-Fi/Bluetooth modules
and firmware. `tegra-configs-udev` plus the desktop user's GPU groups provide
non-root GPU access; generic in-tree modules alone did not cover this hardware.

Firefox reports that the kernel's AppArmor policy denies creation of an
unprivileged user namespace. Rendering works; the policy was not disabled.
Browser sandbox parity with Ubuntu has not been validated.

The SanDisk installer USB was rebuilt on 2026-10-27 with this desktop payload
and maintenance image `qdma-installer-image-p3509-a02-p3767-0000.rootfs-20261027104523.tegraflash-tar.zst`.
The maintenance image build passed all 5,811 tasks. The payload's compressed and
decompressed SHA-256 hashes were verified by reading back from USB. The stick
also includes this repository and instructions under `/opt/jetson-yocto-qdma`.

This desktop archive has not undergone another clean flash or USB installation
cycle. Host readback checks do not establish that the newly written stick boots
or that installation completes on the Orin. Follow the installation guide to
boot the USB, run `qdma-install --dry-run`, and explicitly start installation.
