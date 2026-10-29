# Native QDMA development on the Orin

This directory contains the complete QDMA Linux kernel-driver and utility source
subtree from Xilinx dma_ip_drivers revision recorded in UPSTREAM_REVISION. All
five project patches are already applied and retained in patches/ for reference.
Do not apply them again. Sources for unrelated NVIDIA/Ubuntu applications are
not included here.

The image includes GCC, make, libc development files, libaio development headers
and library, and kernel-devsrc matching the image kernel. Build in a writable
copy so the packaged sources remain available:

```sh
cp -a /usr/src/qdma ~/qdma-work
cd ~/qdma-work
sh build-tools.sh
```

The six binaries appear in linux-kernel/bin. This does not replace /usr/bin or
load a driver. Hardware transfers require a compatible FPGA bitstream and driver.
To install selected rebuilt utilities deliberately, use `sudo install -m 0755
linux-kernel/bin/TOOL /usr/local/bin/TOOL`; /usr/local/bin may take precedence over
packaged /usr/bin versions. Keep track of local overrides.

## Kernel module build

The source directory includes driver/ and its shared headers. Use the matching
kernel development tree, never headers for a different kernel release:

```sh
uname -r
readlink -f /lib/modules/$(uname -r)/build
sudo make -C /lib/modules/$(uname -r)/build scripts prepare
cd ~/qdma-work/linux-kernel
make driver KSRC=/lib/modules/$(uname -r)/build \
    KOBJ=/lib/modules/$(uname -r)/build USER_ARCH=arm64 MODULE=mod_pf \
    CC=gcc LD=ld AR=ar CROSS_COMPILE=
```

This is the QDMA external-module build, not a full NVIDIA kernel rebuild.
`kernel-devsrc` supplies the prepared kernel development tree; it does not ship
all NVIDIA out-of-tree driver sources. Do not use upstream `make install-mods`
blindly. Review the built bin/qdma-pf.ko with modinfo before replacing any running
module. Building a module does not validate PCIe hardware behavior.
