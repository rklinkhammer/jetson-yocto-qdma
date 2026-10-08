# Jetson Yocto + Xilinx QDMA

One Docker container builds a minimal console image for Orin NX or Orin Nano,
including the Xilinx QDMA PF kernel module and six QDMA userspace tools.

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

This is a development image: serial-console root login has no password and no
SSH server is added. QDMA is installed but not automatically loaded. Board PCIe
routing, endpoint compatibility, and runtime DMA operation still need hardware
validation. The image build includes NVIDIA BSP components under their upstream
licenses; it does not apply the old project's open-source-only build restriction.

## Checked

The Docker image builds successfully. Full-image BitBake dry runs passed for
Orin NX and the Orin Nano NVMe target, and real QDMA fetch/unpack/patch tasks
passed. A complete image compilation and hardware boot have not been run.

The hash-server configuration was checked with a real Unix-socket report/query
from the build container, persistence across a server restart, both named-volume
and local bind-mount storage, and BitBake's effective configuration. The other
system's NFS filesystem has not been tested here.

Automatic identity was checked using the invoking account and a different
UID/GID with supplementary groups. Host file ownership, Git access, hash-server
communication, database persistence across a user change, and BitBake environment
initialization passed. Existing NFS permissions remain the host administrator's
responsibility.
