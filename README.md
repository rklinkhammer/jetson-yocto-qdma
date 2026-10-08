# Jetson Yocto + Xilinx QDMA

One Docker container builds a minimal console image for Orin NX or Orin Nano,
including the Xilinx QDMA PF kernel module and six QDMA userspace tools.

## Build

Requires Docker Engine with Compose on an x86-64 Linux host. The container runs
as UID/GID 1000, matching the owner of this project. Reserve roughly 150 GB of
free disk space for an initial build; downloads and compiled state are reused.

Orin NX 16 GB on the NVIDIA P3768 carrier (default):

```sh
docker compose run --build --rm yocto
```

Orin Nano developer kit with an NVMe root filesystem:

```sh
MACHINE=jetson-orin-nano-devkit-nvme docker compose run --build --rm yocto
```

For the Nano developer kit's SD-card configuration, use
`MACHINE=jetson-orin-nano-devkit` instead. These are Orin Nano targets, not the
original Tegra210 Jetson Nano. Select the machine matching your module/carrier.

The container initializes the build and runs `bitbake qdma-image`.
Images and flash bundles appear in `build/<MACHINE>/tmp/deploy/images/<MACHINE>/`.
Nothing is flashed automatically.

## Files

- `Dockerfile`: Ubuntu 24.04 build tools and pinned upstream Yocto sources.
- `compose.yaml`: one container with this directory mounted at `/project`.
- `build.sh`: initializes the machine's build directory and invokes BitBake.
- `local.conf`: shared BitBake settings; edit this file to change build policy.
- `meta-qdma/`: the image, small distro configuration, and QDMA recipes/patches.

`build/`, `downloads/`, and `sstate-cache/` are persistent host directories.
The generated files under `build/<MACHINE>/conf/` are replaced on each run;
make configuration changes in the project's `local.conf` or layer instead.
The defaults use four CPUs, 12 GiB container memory, and two BitBake workers.

Ordinary BitBake arguments can follow the service name, for example:

```sh
docker compose run --rm yocto -n qdma-image  # dry run
docker compose run --rm yocto qdma-tools    # tools only
```

## Sources and image scope

[OE4T tegra-demo-distro](https://github.com/OE4T/tegra-demo-distro/tree/6eab4529e8ba83231214677dc70f504ca1cb326f)
pins the Wrynose OE-Core, BitBake, meta-openembedded, and meta-tegra revisions.
Only those four repositories are included. This uses the R39.2.1 BSP and its
NVIDIA kernel through [meta-tegra](https://github.com/OE4T/meta-tegra/tree/322bc23e5da0d6100a885660b144567182fcd312).
QDMA is pinned to `b8466090b4e812e191da9e9305ffb11cb7ace768` with the existing
ARM64 timestamp, Kbuild, compiler, and userspace fixes.

This is a development image: serial-console root login has no password and no
SSH server is added. QDMA is installed but not automatically loaded. Board PCIe
routing, endpoint compatibility, and runtime DMA operation still need hardware
validation. The image build includes NVIDIA BSP components under their upstream
licenses; it does not apply the old project's open-source-only build restriction.

## Checked

The Docker image builds successfully. Full-image BitBake dry runs passed for
Orin NX and the Orin Nano NVMe target, and real QDMA fetch/unpack/patch tasks
passed. A complete image compilation and hardware boot have not been run.
