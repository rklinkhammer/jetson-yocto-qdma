#!/usr/bin/env python3
"""Prepare a manually installed C0 boot configuration from a matching image root."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('rootfs', type=Path)
p.add_argument('output', type=Path)
a = p.parse_args()
repo = Path(__file__).resolve().parents[1]
a.output.mkdir(parents=True, exist_ok=True)
conf = (a.rootfs / 'boot/extlinux/extlinux.conf').read_text()
fdt = re.findall(r'^\s*FDT\s+(\S+)\s*$', conf, re.M)
if len(fdt) != 1 or not fdt[0].startswith('/boot/'):
    raise SystemExit('Expected exactly one absolute /boot/ FDT entry')
if re.search(r'^\s*(?:FDTOVERLAYS|OVERLAYS)\s', conf, re.M):
    raise SystemExit('Merge/review existing overlays before preparing this configuration')
base = a.output / 'base.dtb'
shutil.copyfile(a.rootfs / fdt[0].lstrip('/'), base)
(a.output / 'extlinux.conf.original').write_text(conf)
source = a.output / 'pci0-clock-request.dts'
shutil.copyfile(repo / 'meta-qdma/recipes-bsp/pci0-overlay/files/pci0-comment-overlay.dts', source)

def run(*args):
    return subprocess.check_output(list(map(str, args)), text=True).strip()

def nodes(dtb, node='/'):
    yield node
    for child in run('fdtget', '-l', dtb, node).splitlines():
        yield from nodes(dtb, node.rstrip('/') + '/' + child)

def properties(dtb, node):
    return run('fdtget', '-p', dtb, node).splitlines()

prop = 'nvidia,enable-power-down'
removed = [n for n in nodes(base) if prop in properties(base, n)]
clean = a.output / 'base-no-power-down.dtb'
shutil.copyfile(base, clean)
for node in removed:
    run('fdtput', '-d', clean, node, prop)
overlay = a.output / 'pci0-clock-request.dtbo'
merged = a.output / 'pcie-c0-no-power-down.dtb'
run('dtc', '-@', '-I', 'dts', '-O', 'dtb', '-o', overlay, source)
run('fdtoverlay', '-i', clean, '-o', merged, overlay)
all_nodes = list(nodes(merged))
assert not any(prop in properties(merged, n) for n in all_nodes)
target = '/bus@0/pcie@14180000'
assert 'nvidia,disable-clock-request' in properties(merged, target)
assert run('fdtget', '-t', 'bx', merged, target, 'nvidia,disable-clock-request') == ''
new_conf = re.sub(r'^(\s*FDT\s+)\S+\s*$', r'\g<1>/boot/dtb/pcie-c0-no-power-down.dtb', conf, flags=re.M)
(a.output / 'extlinux.conf').write_text(new_conf)
report = dict(base_fdt=fdt[0], removed_from_nodes=removed, nodes_checked=len(all_nodes),
              power_down_absent=True, clock_request_disabled_on=target,
              target_status=run('fdtget', '-t', 's', merged, target, 'status'),
              sha256={x.name: hashlib.sha256(x.read_bytes()).hexdigest()
                      for x in [base, clean, source, overlay, merged]})
(a.output / 'verification.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
