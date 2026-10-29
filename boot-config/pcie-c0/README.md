# PCI0/C0 clock-request boot configuration

Prepared for the Yocto Orin NX 16 GB/P3509 release
`qdma-image-p3509-a02-p3767-0000.rootfs-20261029094141` (L4T R39.2.1).
Nothing in this directory has been installed or activated on the Jetson.

## Contents and behavior

- `pci0-clock-request.dts` and `.dtbo`: source and compiled overlay adding the
  boolean `nvidia,disable-clock-request` to `/bus@0/pcie@14180000` (controller C0).
- `base.dtb`: unmodified DTB from the release, retained for comparison.
- `base-no-power-down.dtb`: base with any `nvidia,enable-power-down` properties
  removed before overlay application. For this release it is identical to base.
- `pcie-c0-no-power-down.dtb`: verified base plus overlay, ready for `/boot/dtb/`.
- `extlinux.conf`: copy of the release boot configuration, changing only its FDT
  filename to `/boot/dtb/pcie-c0-no-power-down.dtb`.
- `extlinux.conf.original`: original release configuration for comparison.
- `verification.json`: source hashes and the verification results.

All 1,212 nodes in the final DTB were checked: `nvidia,enable-power-down` is absent
throughout the tree, including all 11 PCIe controller nodes. A decompiled diff
shows exactly one addition: `nvidia,disable-clock-request` on C0. This property is
not added to the other controllers.

**C0 has `status = "disabled"` in this base DTB and remains disabled.** Linux PCI
bus numbering is not sufficient to identify the physical controller. This change
does not enable C0, select a QDMA slot or change lane/pinmux configuration.

A conventional compiled overlay cannot encode deletion of an existing base-tree
property with `/delete-property/`. The preparation script therefore sanitizes the
base DTB, applies the overlay offline, and verifies the resulting whole tree.
The supplied extlinux file selects that merged DTB; it needs no overlay-loader
directive. Using just the DTBO with an arbitrary base cannot guarantee absence
of `nvidia,enable-power-down`.

## Install manually on the matching Jetson

Copy this whole directory to the Jetson, then open a root shell in that copy.
Before installation, compare the current boot configuration and base DTB:

```sh
diff -u extlinux.conf.original /boot/extlinux/extlinux.conf
cmp base.dtb /boot/dtb/tegra234-p3768-0000+p3767-0000-nv-super.dtb
```

Both commands must show no differences for an exact match. If either differs,
stop and regenerate from that installation's matching `/boot` files, or review
and preserve its boot settings explicitly. Do not replace a custom root-device
argument or use this DTB on a different module/carrier/BSP.

Back up the actual configuration, then copy the merged DTB and install the new
configuration last:

```sh
backup="/boot/extlinux/extlinux.conf.before-pcie-c0-$(date +%Y%m%d-%H%M%S)"
cp -a /boot/extlinux/extlinux.conf "$backup"
echo "Rollback configuration: $backup"
install -m 0644 pcie-c0-no-power-down.dtb /boot/dtb/pcie-c0-no-power-down.dtb
install -m 0644 extlinux.conf /boot/extlinux/extlinux.conf.new
mv /boot/extlinux/extlinux.conf.new /boot/extlinux/extlinux.conf
sync
reboot
```

The configuration belongs in **`/boot/extlinux/extlinux.conf`**, not directly in
`/boot`. Keep the original board DTB in place. The separate `.dtbo` is supplied
for development; it need not be copied for this merged-DTB method.

## Verify after boot

The DTC-enabled release includes the tools used below. Check the actual live
tree, since firmware can apply additional changes during boot:

```sh
find /sys/firmware/devicetree/base -name 'nvidia,enable-power-down' -print
test -e /sys/firmware/devicetree/base/bus@0/pcie@14180000/nvidia,disable-clock-request \
    && echo 'C0 disable-clock-request present'
tr -d '\000' < /sys/firmware/devicetree/base/bus@0/pcie@14180000/status
lspci -nn
```

The first command must print nothing. The property is boolean/zero-length, so
use `test -e`, not a nonempty-file test. C0 should still report `disabled`.
This prepared configuration has passed offline validation, not a hardware boot
or PCIe-link test.

## Roll back

Restore the backup path printed during installation, then sync and reboot:

```sh
cp -a /boot/extlinux/extlinux.conf.before-pcie-c0-YOUR_TIMESTAMP /boot/extlinux/extlinux.conf
sync
reboot
```

If the OS cannot boot, use the working USB maintenance installer to mount the
NVMe root filesystem and restore that file there. Do not run `qdma-install` for
this rollback; reinstalling would erase the existing filesystem.

## Rebuild for a matching filesystem

From the repository root on a host with `dtc`, `fdtget`, `fdtput` and `fdtoverlay`:

```sh
scripts/prepare-pcie-boot-config.py /path/to/matching/rootfs /path/to/new-output
```

The rootfs must contain `/boot/extlinux/extlinux.conf` and its referenced DTB.
The script copies the base, removes all instances of the unwanted property,
compiles/applies the C0 overlay and verifies the merged tree. It does not install
anything. Retain these instructions with the generated files.
