#!/bin/bash
# Build a compact whole-NVMe image using the release's own partition writer.
set -euo pipefail
if [[ $# != 2 ]]; then
    echo "Usage: $0 RELEASE.tegraflash-tar.zst NEW_OUTPUT_DIRECTORY" >&2
    exit 2
fi
release=$(realpath "$1")
output=$(realpath -m "$2")
[[ ! -e "$output" ]] || { echo 'Output directory already exists' >&2; exit 1; }
mkdir -p "$output/bundle"
tar --zstd -xf "$release" -C "$output/bundle"
grep -qx 'MACHINE="p3509-a02-p3767-0000"' "$output/bundle/.env.initrd-flash"
(
    cd "$output/bundle"
    # Offline signing only; no Jetson is contacted by doexternal.sh.
    ./doexternal.sh -s 12G "$output/nvme.img"
) > "$output/prepare.log" 2>&1
pigz -1 -k "$output/nvme.img"
python3 - "$output" "$release" <<'PY'
import hashlib, json, pathlib, sys
out, release = map(pathlib.Path, sys.argv[1:])
def sha(p):
    with p.open('rb') as f:
        return hashlib.file_digest(f, 'sha256').hexdigest()
data = dict(machine='p3509-a02-p3767-0000', release=release.name,
            bundle_sha256=sha(release), raw_bytes=(out/'nvme.img').stat().st_size,
            raw_sha256=sha(out/'nvme.img'), gzip_sha256=sha(out/'nvme.img.gz'))
(out/'release.json').write_text(json.dumps(data, indent=2)+'\n')
print(json.dumps(data, indent=2))
PY
echo "Payload ready: $output/nvme.img.gz"
