# Device-tree tools on the Jetson

The DTC release extends the Xfce/CUDA filesystem with `dtc`, `dtc-misc` and
`dtc-dev`. These provide:

- `dtc`: compile DTS to DTB/DTBO, decompile DTB, and emit YAML or assembler.
- `fdtdump`: inspect a flattened device tree.
- `fdtget` and `fdtput`: read and edit properties in a DTB file.
- `fdtoverlay`: apply compiled overlays to a base DTB file.
- `dtdiff`: compare device trees, and `convert-dtsv0`: convert legacy DTS syntax.
- `libfdt` and its C headers/link library for building device-tree utilities.

The existing native C toolchain provides preprocessing for sources using C-style
`#include` directives or macros. These are target tools installed on the Jetson,
not just tools available inside the host's Yocto build container.

## Compile an overlay

```sh
dtc -@ -I dts -O dtb -o my-overlay.dtbo my-overlay.dts
```

The previously built release includes a comment-only PCI0 template under
`/usr/share/qdma/device-tree/`. The updated source adds the C0 clock-request
property without enabling the controller. See the prepared
[PCI0 boot configuration](../boot-config/pcie-c0/README.md) for compiled files
and manual installation instructions.

For sources that require C preprocessing (supply the matching include tree):

```sh
cpp -nostdinc -undef -D__DTS__ -x assembler-with-cpp \
    -I /path/to/matching/includes my-overlay.dts > my-overlay.pp.dts
dtc -@ -I dts -O dtb -o my-overlay.dtbo my-overlay.pp.dts
```

Board-specific kernel DTS include files are not supplied by the DTC package.
Use the sources matching this image's NVIDIA kernel and board configuration.

## Inspect and test files

```sh
dtc -I dtb -O dts -o decoded.dts base.dtb
fdtget -t s base.dtb / compatible
fdtoverlay -i base.dtb -o merged.dtb my-overlay.dtbo
dtdiff base.dtb merged.dtb
```

Symbol-based overlays need symbols in the base DTB (built with `-@`). An overlay
using `target-path` instead needs that exact path to exist in the base tree.
These commands operate on files; they do not activate an overlay in the running
kernel. Boot configuration changes are a separate board-specific operation.

Run the repository's functional check on the Jetson:

```sh
sh scripts/validate-device-tree.sh
```

It compiles and applies a synthetic overlay, reads/edits properties, checks a
DTB round trip and YAML output, and compiles/runs a small `libfdt` C program.
It uses temporary files and does not change boot configuration.

## Release artifacts and validation (2026-10-29)

Machine: `p3509-a02-p3767-0000` (Orin NX 16 GB/P3509). Artifacts are in
`build/p3509-a02-p3767-0000/tmp/deploy/images/p3509-a02-p3767-0000/`:

```text
qdma-image-p3509-a02-p3767-0000.rootfs-20261029094141.ext4
SHA256 ece2ef9a5a5474b29a48e6b3625faf22e47c61a785d389a08b0074923d5a3a57

qdma-image-p3509-a02-p3767-0000.rootfs-20261029094141.tegraflash-tar.zst
SHA256 c6b115304d3bb55332877bcba62653374ca898bf5906e6484aa65ce35dccb38a
```

All 13,120 BitBake tasks passed. The manifest contains `dtc`, `dtc-misc` and `dtc-dev` version 1.7.2-r0.
The functional script passed using ARM64 emulation with the generated root
filesystem mounted read-only and temporary `/tmp`, `/dev` and `/proc` mounts.
This verifies userspace tool execution, not hardware boot or a real PCIe overlay.

The existing Jetson installation and installer USB sticks have not been updated
with this release. Use the complete flash bundle for recovery flashing, or
prepare a new USB payload following [INSTALLATION.md](INSTALLATION.md). The ext4
file alone does not contain the partition table or boot firmware.
