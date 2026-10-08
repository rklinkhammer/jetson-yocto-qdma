#!/bin/bash
set -euo pipefail
: "${HOST_UID:?Run the build with ./yocto to supply your host identity}"
: "${HOST_GID:?Run the build with ./yocto to supply your host identity}"
[[ $HOST_UID =~ ^[0-9]+$ && $HOST_GID =~ ^[0-9]+$ && $HOST_UID != 0 ]] || {
    echo 'HOST_UID must be non-root and HOST_GID must be numeric.' >&2; exit 1;
}
HOST_GROUPS=${HOST_GROUPS:-$HOST_GID}
[[ $HOST_GROUPS =~ ^[0-9]+(,[0-9]+)*$ ]] || { echo 'Invalid HOST_GROUPS' >&2; exit 1; }
# Supply a passwd entry for tools that resolve the build user's name.
getent group "$HOST_GID" >/dev/null || groupadd --gid "$HOST_GID" "hostgroup-$HOST_GID"
getent passwd "$HOST_UID" >/dev/null || useradd --uid "$HOST_UID" --gid "$HOST_GID"     --no-create-home --home-dir /home/yocto --shell /bin/bash "hostuser-$HOST_UID"
install -d -o "$HOST_UID" -g "$HOST_GID" /home/yocto
export HOME=/home/yocto
export USER=$(getent passwd "$HOST_UID" | cut -d: -f1)
export LOGNAME=$USER
# Only the server owns its local database files; never chown /project (NFS).
if [[ ${1:-} == /opt/yocto/repos/bitbake/bin/bitbake-hashserv ]]; then
    shopt -s nullglob
    chown "$HOST_UID:$HOST_GID" /hashserv /hashserv/hashserv.db* /hashserv/hashserv.sock*
fi
exec setpriv --reuid="$HOST_UID" --regid="$HOST_GID" --groups="$HOST_GROUPS" "$@"
