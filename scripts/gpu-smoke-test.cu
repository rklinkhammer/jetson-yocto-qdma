// Compile and run on the Jetson to check the native toolchain and GPU libraries.
#include <cuda_runtime.h>
#include <cudnn.h>
#include <NvInfer.h>
#include <cstdio>
#include <memory>

class Logger : public nvinfer1::ILogger {
    void log(Severity severity, char const* message) noexcept override {
        if (severity <= Severity::kWARNING) std::fprintf(stderr, "TensorRT: %s\n", message);
    }
};

__global__ void add(int* value) { *value += 35; }

int main() {
    int count = 0;
    auto result = cudaGetDeviceCount(&count);
    if (result != cudaSuccess || count == 0) {
        std::fprintf(stderr, "No usable CUDA GPU: %s\n", cudaGetErrorString(result));
        return 1;
    }
    cudaDeviceProp properties{};
    if (cudaGetDeviceProperties(&properties, 0) != cudaSuccess) return 1;
    std::printf("CUDA GPU: %s, compute capability %d.%d\n",
                properties.name, properties.major, properties.minor);
    int value = 7;
    int* device = nullptr;
    if (cudaMalloc(&device, sizeof(value)) != cudaSuccess) return 1;
    if (cudaMemcpy(device, &value, sizeof(value), cudaMemcpyHostToDevice) != cudaSuccess) return 1;
    add<<<1, 1>>>(device);
    if (cudaGetLastError() != cudaSuccess || cudaDeviceSynchronize() != cudaSuccess) return 1;
    if (cudaMemcpy(&value, device, sizeof(value), cudaMemcpyDeviceToHost) != cudaSuccess) return 1;
    cudaFree(device);
    if (value != 42) return 1;
    std::puts("CUDA kernel result: PASS (42)");
    cudnnHandle_t handle;
    auto status = cudnnCreate(&handle);
    if (status != CUDNN_STATUS_SUCCESS) {
        std::fprintf(stderr, "cuDNN: %s\n", cudnnGetErrorString(status));
        return 1;
    }
    std::printf("cuDNN handle: PASS (version %zu)\n", cudnnGetVersion());
    cudnnDestroy(handle);
    Logger logger;
    std::unique_ptr<nvinfer1::IBuilder> builder(nvinfer1::createInferBuilder(logger));
    if (!builder) return 1;
    std::printf("TensorRT builder: PASS (headers %d.%d.%d)\n",
                NV_TENSORRT_MAJOR, NV_TENSORRT_MINOR, NV_TENSORRT_PATCH);
    return 0;
}
