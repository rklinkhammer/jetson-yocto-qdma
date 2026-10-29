#!/bin/sh
# Exercise installed target tools without modifying the running device tree.
set -eu
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
cd "$tmp"
for tool in dtc fdtdump fdtget fdtput fdtoverlay dtdiff convert-dtsv0 cc; do
    command -v "$tool"
done
cat > base.dts <<'DTS'
/dts-v1/;
/ {
    compatible = "qdma,dtc-test";
    testnode { status = "disabled"; };
};
DTS
cat > overlay.dts <<'DTS'
/dts-v1/;
/plugin/;
/ {
    fragment@0 {
        target-path = "/testnode";
        __overlay__ { status = "okay"; test-value = <42>; };
    };
};
DTS
dtc -@ -I dts -O dtb -o base.dtb base.dts
dtc -@ -I dts -O dtb -o overlay.dtbo overlay.dts
fdtoverlay -i base.dtb -o merged.dtb overlay.dtbo
[ "$(fdtget -t s merged.dtb /testnode status)" = okay ]
[ "$(fdtget -t u merged.dtb /testnode test-value)" = 42 ]
fdtput -t s merged.dtb /testnode status disabled
[ "$(fdtget -t s merged.dtb /testnode status)" = disabled ]
fdtdump merged.dtb > dump.txt 2>/dev/null
dtc -I dtb -O dts -o roundtrip.dts merged.dtb
dtc -I dts -O dtb -o roundtrip.dtb roundtrip.dts
dtdiff merged.dtb roundtrip.dtb
dtc -I dts -O yaml -o base.yaml base.dts
test -s base.yaml
cat > libfdt-check.c <<'C'
#include <libfdt.h>
#include <stdio.h>
int main(void) {
    char blob[1024];
    int rc = fdt_create_empty_tree(blob, sizeof(blob));
    if (rc || fdt_check_header(blob)) return 1;
    puts("libfdt headers/link/runtime: PASS");
    return 0;
}
C
cc libfdt-check.c -lfdt -o libfdt-check
./libfdt-check
echo DEVICE_TREE_TOOLS_VALIDATION_PASSED
