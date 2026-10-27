#!/bin/sh
set -eu
source_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
export PATH=/usr/local/cuda-13.2/bin:$PATH
test_dir=$(mktemp -d /tmp/qdma-gpu.XXXXXX)
trap 'rm -rf "$test_dir"' EXIT

nvcc --version
g++ --version | head -n 1
cmake --version | head -n 1
ninja --version
git --version
gdb --version | head -n 1
/usr/bin/cuda-samples/deviceQuery
/usr/bin/cuda-samples/vectorAdd
nvcc -std=c++17 -arch=sm_87 -isystem /usr/include/cuda-compat-workarounds \
    "$source_dir/gpu-smoke-test.cu" -lcudnn -lnvinfer -o "$test_dir/gpu-smoke-test"
"$test_dir/gpu-smoke-test"
/usr/src/tensorrt/bin/trtexec --help > "$test_dir/trtexec-help.txt" 2>&1
echo 'CUDA_DEVELOPMENT_VALIDATION_PASSED'
