#!/bin/sh
# Run from a writable copy of this directory; binaries remain local until installed.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/linux-kernel"
make -C apps clean
mkdir -p bin
for app in dma-ctl dma-from-device dma-to-device dma-xfer dma-perf dma-latency; do
    make -j1 -C "apps/$app" "$app" CC="${CC:-gcc}" \
        CFLAGS="${CFLAGS:--O2 -g} -I. -I../include -I../dma-utils" LDFLAGS="${LDFLAGS:-}"
    install -m 0755 "apps/$app/$app" "bin/$app"
done
echo "Built all six QDMA utilities in $(pwd)/bin"
