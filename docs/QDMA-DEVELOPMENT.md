# On-device QDMA builds

The development release adds the complete patched QDMA Linux driver/application
subtree at `/usr/src/qdma/linux-kernel`, the applied patches, pinned upstream
revision, build helper and instructions. It includes `libaio-dev`, the native
compiler/toolchain, and `kernel-devsrc` matching kernel
`6.8.12-l4t-r39.2.1-1021.21`, plus kernel build dependencies.

On the installed Jetson:

```sh
cp -a /usr/src/qdma ~/qdma-work
cd ~/qdma-work
sh build-tools.sh
```

This builds dma-ctl, dma-from-device, dma-to-device, dma-xfer, dma-perf and
dma-latency into `linux-kernel/bin/` without replacing `/usr/bin/` utilities.
Read `/usr/src/qdma/README.md` for the separate driver-module procedure.

All six utilities rebuilt successfully using the generated ARM64 root filesystem
under QEMU on 2026-10-29. The test used that filesystem's GCC, make, headers and
libraries, with a writable temporary copy of the packaged sources. It did not
exercise an FPGA, load the rebuilt driver, or validate native driver compilation.
Those hardware/module checks remain to be performed on the Orin.

The included sources cover the QDMA Linux driver and tools, not every package in
the distribution. The kernel development package is intended for external module
builds, not a complete set of NVIDIA's out-of-tree driver source repositories.

The same image includes DTC tools and the optional prepared PCIe configuration at
`/usr/share/qdma/device-tree/pcie-c0/`. Its alternate extlinux configuration is not
activated automatically. Confirm the normal USB installation first, then use the
included README to test the PCIe configuration separately.
