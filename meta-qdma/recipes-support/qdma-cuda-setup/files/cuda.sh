# CUDA version follows the pinned meta-tegra toolkit recipes.
case ":$PATH:" in
    *:/usr/local/cuda-13.2/bin:*) ;;
    *) PATH="/usr/local/cuda-13.2/bin:$PATH" ;;
esac
case ":$PATH:" in
    *:/usr/src/tensorrt/bin:*) ;;
    *) PATH="/usr/src/tensorrt/bin:$PATH" ;;
esac
export PATH
