# Jetson Yocto + Xilinx QDMA

For initial setup, a clean recovery flash, creating a USB-to-NVMe installer,
and checking the installed system, follow the
[step-by-step installation guide](docs/INSTALLATION.md).
Hardware test results are recorded in [installation validation](docs/INSTALLATION-VALIDATION.md).

One Docker container builds an Xfce desktop and CUDA development image for Orin NX or Orin Nano,
including the Xilinx QDMA PF kernel module, six QDMA userspace tools, and
Micro-USB serial console support through `l4t-usb-device-mode`.
PCI diagnostics include the full `pciutils` package and PCI ID database, plus
`pcimem` for PCI BAR access. The kernel enables PCIe, MSI, AER, SR-IOV, and IOMMU
support.

The image also includes the NVIDIA GPU driver, CUDA toolkit (including `nvcc`,
CUDA-GDB and Compute Sanitizer), CUDA samples, cuDNN and TensorRT libraries and
headers, TensorRT plugins/ONNX parser and `trtexec`. Native development tools
include GCC/G++, make, CMake, Ninja, Git, GDB/gdbserver, strace and ltrace.
These packages come from the pinned meta-tegra recipes; this remains a Yocto
distribution rather than an Ubuntu/SDK Manager installation. The desktop adds
Xfce, Firefox ESR (Mozilla’s official ARM64 binary), NetworkManager, audio controls, OpenSSH and common administration
tools. See [desktop and SSH setup](docs/DESKTOP.md) for startup and login instructions.
`qdma-cuda-setup` loads both `nvgpu` and NVIDIA's out-of-tree `tegra-drm` at
boot. CUDA needs the host1x DRM render node even on a headless system; omitting
`tegra-drm` caused `cudaGetDeviceCount` to return error 801 on the P3509.
Login shells also receive the CUDA and TensorRT tool directories in `PATH`.

`tegra-redundant-boot` is required even for the single NVMe root filesystem:
its `nv_update_verifier` service confirms a successful boot to NVIDIA firmware.
Without it, the firmware can exhaust its boot retries and select recovery.

## CUDA development on the Jetson

For the currently pinned CUDA 13.2 recipes, set up a console session with:

```sh
export PATH=/usr/local/cuda-13.2/bin:$PATH
export CUDACXX=/usr/local/cuda-13.2/bin/nvcc
. /environment-setup.d/cuda_target.sh
nvcc --version
/usr/bin/cuda-samples/deviceQuery
/usr/bin/cuda-samples/vectorAdd
/usr/src/tensorrt/bin/trtexec --help
```

The pinned layer provides compatibility headers for its newer glibc. For native
CUDA compilation, include them explicitly and select Orin's GPU architecture:

```sh
nvcc -arch=sm_87 -isystem /usr/include/cuda-compat-workarounds example.cu -o example
```

With this repository on the Jetson, the combined hardware smoke test is:

```sh
nvcc -std=c++17 -arch=sm_87 -isystem /usr/include/cuda-compat-workarounds \
    scripts/gpu-smoke-test.cu -lcudnn -lnvinfer -o /tmp/gpu-smoke-test
/tmp/gpu-smoke-test
```

It checks a CUDA kernel result, cuDNN handle creation and TensorRT builder
creation. It does not benchmark performance or validate a complete AI model.
Run `sh scripts/validate-gpu.sh` for this test plus NVIDIA's `deviceQuery` and
`vectorAdd` samples and tool-version checks.

Image build success verifies packaging; GPU execution and native compilation
must also be checked on the Jetson after installing the new image. Existing
NVMe installations and previously made USB sticks are not updated by a build.

## Build

Requires Docker Engine with Compose on an x86-64 Linux host. The `./yocto`
launcher automatically passes your current UID, primary GID, and supplementary
groups to both containers. Run it as your normal user, without sudo. Reserve
roughly 150 GB of free disk space for an initial build; downloads and compiled
state are reused.

Orin NX 16 GB on the NVIDIA P3768 carrier (default):

```sh
./yocto
```

Orin NX 16 GB on the P3509 carrier with an NVMe root filesystem:

```sh
MACHINE=p3509-a02-p3767-0000 ./yocto
```

This target includes a USB device-role fallback matching NVIDIA's stock startup:
if role detection leaves the console port at `none`, select `device` after the
USB gadget starts. This allows Micro-USB serial login without a monitor.

Orin Nano developer kit with an NVMe root filesystem:

```sh
MACHINE=jetson-orin-nano-devkit-nvme ./yocto
```

For the Nano developer kit's SD-card configuration, use
`MACHINE=jetson-orin-nano-devkit` instead. These are Orin Nano targets, not the
original Tegra210 Jetson Nano. Select the machine matching your module/carrier.

The container initializes the build and runs `bitbake qdma-image`.
Images and flash bundles appear in `build/<MACHINE>/tmp/deploy/images/<MACHINE>/`.
Nothing is flashed automatically.

## Files

- `Dockerfile`: Ubuntu 24.04 build tools and pinned upstream Yocto sources.
- `compose.yaml`: the build container and a local Unix-socket hash server.
- `yocto`: detects your host identity and runs Compose.
- `container-entrypoint.sh`: creates the container account and drops privileges.
- `build.sh`: initializes the machine's build directory and invokes BitBake.
- `local.conf`: shared BitBake settings; edit this file to change build policy.
- `meta-qdma/`: the image, small distro configuration, and QDMA recipes/patches.

`build/`, `downloads/`, and `sstate-cache/` are persistent host directories.
The generated files under `build/<MACHINE>/conf/` are replaced on each run;
make configuration changes in the project's `local.conf` or layer instead.
The defaults use four CPUs, 12 GiB container memory, and two BitBake workers.

Containers briefly start as root to establish the account, then drop to your
numeric UID/GID and supplementary groups. No image rebuild is needed just to
change users. The hash server adjusts ownership only of its dedicated directory
and database/socket files. Project files and existing build/cache trees are never
recursively chowned; files left by a previous user still need appropriate shared
group/ACL permissions. The displayed container username may differ from the host
username; filesystem access is determined by the numeric IDs.

Use one invoking user for the shared hash server at a time. Switching users
recreates the service and changes ownership of its database, so finish active
builds first. This does not configure Docker daemon access or bypass NFS ACLs.

Ordinary BitBake arguments can follow `./yocto`, for example:

```sh
./yocto -n qdma-image  # dry run
./yocto qdma-tools    # tools only
```

## Hash server and NFS

The normal build command also starts `bitbake-hashserv` and waits for it to be
healthy. Builds connect to `unix:///hashserv/hashserv.sock`. Only the hash server
opens `/hashserv/hashserv.db`; builders access the service through the socket.
No TCP port is exposed. The server remains running between builds; stop it with
`docker compose down` (its database is preserved).

By default, the database and socket live in the separate Docker volume
`jetson-yocto-qdma_hashserv`. Docker's volume storage must be on a local filesystem.
To choose a specific non-NFS directory on the Docker host, create it once:

```sh
sudo install -d -o "$(id -u)" -g "$(id -g)" -m 0750 /var/lib/jetson-yocto-hashserv
```

Add this setting to the project's `.env` file on that system:

```dotenv
HASHSERVE_DIR=/var/lib/jetson-yocto-hashserv
```

Then use the same `./yocto` command. The directory
must exist, be writable by your user, and reside on local storage. The Unix
socket works between containers on that Docker host, not between separate hosts.
Each host needs its own local server/database with this configuration.

`SSTATE_DIR` stays at `/project/sstate-cache` and can be on NFS. This change only
moves the hash database and socket; Yocto's `TMPDIR` also needs a supported local
filesystem if the project itself is on NFS. Set `TMPDIR` in `local.conf` to a
local path mounted into the build container in that case.

Existing `sstate-cache/hashserv.db` files are not moved or deleted. The new
server starts with an empty database; existing sstate artifacts remain intact.
Do not share this database directory with another server or copy a live SQLite
database. See the [Yocto hash server documentation](https://docs.yoctoproject.org/dev/dev-manual/hashequivserver.html).

## Sources and image scope

[OE4T tegra-demo-distro](https://github.com/OE4T/tegra-demo-distro/tree/6eab4529e8ba83231214677dc70f504ca1cb326f)
pins the Wrynose OE-Core, BitBake, meta-openembedded, and meta-tegra revisions.
Only those four repositories are included. This uses the R39.2.1 BSP and its
NVIDIA kernel through [meta-tegra](https://github.com/OE4T/meta-tegra/tree/322bc23e5da0d6100a885660b144567182fcd312).
QDMA is pinned to `b8466090b4e812e191da9e9305ffb11cb7ace768` with the existing
ARM64 timestamp, Kbuild, compiler, and userspace fixes.

This is a development image: serial-console root login has no password. OpenSSH
is included, but rejects empty passwords; configure a password or public key
before remote login. The Micro-USB device port exposes a USB ACM console
(`/dev/ttyACM*` on a Linux host) and USB networking through the upstream
`l4t-usb-device-mode` package. The desktop user is `jetson`; local graphical autologin is enabled.
QDMA is installed but not automatically loaded. Board PCIe
routing, endpoint compatibility, and runtime DMA operation still need hardware
validation. The image build includes NVIDIA BSP components under their upstream
licenses; it does not apply the old project's open-source-only build restriction.

## Checked

The Xfce desktop release passed all 13,101 build tasks and was installed online
on the Orin NX. After reboot, Xfce, SSH, Ethernet and non-root CUDA development
checks passed. Firefox rendering and Wi-Fi/Bluetooth detection were also checked.
See [desktop setup and validation](docs/DESKTOP.md) for the release checksum,
SSH credential setup and the limits of hardware testing.

The CUDA development image was built for `p3509-a02-p3767-0000` and installed
on the running Orin NX 16 GB through USB networking. Native `nvcc` 13.2 and
GCC 15.3 compilation, CUDA `deviceQuery` and `vectorAdd`, cuDNN 9.20 handle
creation, TensorRT 10.16 builder creation, and `trtexec --help` passed. The
same GPU tests passed after a normal NVMe reboot, with automatic GPU/DRM
module loading, working USB console, and successful boot verification.
This does not validate AI model accuracy, sustained performance, or QDMA-to-GPU
transfers.

The Docker image builds successfully. Full-image BitBake dry runs passed for
Orin NX and the Orin Nano NVMe target, and real QDMA fetch/unpack/patch tasks
passed. After correcting the image dependency to install `qdma` (which pulls in
its generated module package), a complete Orin NX image build passed, including
rootfs creation, ext4 output, and the Tegra flashing archive. Its image manifest
contains both the QDMA module and tools. A full Orin Nano build and hardware boot
have not been verified.

The hash-server configuration was checked with a real Unix-socket report/query
from the build container, persistence across a server restart, both named-volume
and local bind-mount storage, and BitBake's effective configuration. The other
system's NFS filesystem has not been tested here.

Automatic identity was checked using the invoking account and a different
UID/GID with supplementary groups. Host file ownership, Git access, hash-server
communication, database persistence across a user change, and BitBake environment
initialization passed. Existing NFS permissions remain the host administrator's
responsibility.

## PCI0 overlay template

The P3509 image includes a comment-only overlay source and compiled DTBO under
`/usr/share/qdma/device-tree/`. It targets Orin controller C0 at
`/bus@0/pcie@14180000` (PCI domain 0), which the base device tree leaves disabled.
It does not enable that controller or identify the attached QDMA slot. The
overlay is supplied as an editable template, not automatically applied at boot.
Comments remain in the DTS source; compilation removes them from the DTBO.

## P3509 display output

Use HDMI with the `p3509-a02-p3767-0000` image. Its current display configuration
exposes HDMI only, not the carrier's DisplayPort connector. The image includes
`nvidia-kernel-oot-display` and `tegra-configs-display-driver` to load NVIDIA's
display modules at boot; CUDA's compute modules alone do not provide display output.

On the running Orin NX, installing these matching packages and connecting HDMI
produced a working framebuffer console. Linux reported the HDMI connector as
connected with 1920x1080 among the supported modes, and CUDA `deviceQuery` and
`vectorAdd` still passed. Those checks were performed on the earlier console release;
[desktop validation](docs/DESKTOP.md#validation) tracks the Xfce update.
A subsequent clean recovery flash also booted with the HDMI framebuffer and USB
console working. HDMI firmware-logo output has not been checked.
