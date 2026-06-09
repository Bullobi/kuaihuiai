#ifndef INFERENCE_ENGINE_H
#define INFERENCE_ENGINE_H

#include <string>
#include <vector>
#include <memory>
#include <functional>

namespace kuaihui {

// 采样器类型
enum class SamplerType {
    EULER = 0,
    EULER_A = 1,
    DDIM = 2,
    PNDM = 3,
    DPM_2M = 4,
    DPM_2M_KARRAS = 5,
    DPM_SDE = 6,
    DPM_SDE_KARRAS = 7,
    LCM = 8,
};

// 推理引擎类型
enum class EngineType {
    CPU = 0,
    GPU_OPENCL = 1,
    GPU_VULKAN = 2,
    NPU_QNN = 3,
    NPU_QCOM = 4,
    ANDROID_NNAPI = 5,
};

/**
 * 推理配置
 */
struct InferenceConfig {
    int width;
    int height;
    int steps;
    float cfgScale;
    int seed;
    float strength;
    SamplerType sampler;
    bool enablePromptCache;
    
    InferenceConfig()
        : width(512), height(512), steps(20), cfgScale(7.5f),
          seed(-1), strength(0.8f), sampler(SamplerType::EULER),
          enablePromptCache(true) {}
};

/**
 * 推理结果
 */
struct InferenceResult {
    uint8_t* imageData;
    int width;
    int height;
    int channels; // 3 for RGB, 4 for RGBA
    bool success;
    std::string errorMessage;
    
    InferenceResult()
        : imageData(nullptr), width(0), height(0), channels(4), success(false) {}
    
    ~InferenceResult() {
        if (imageData != nullptr) {
            delete[] imageData;
            imageData = nullptr;
        }
    }
    
    // 禁用拷贝
    InferenceResult(const InferenceResult&) = delete;
    InferenceResult& operator=(const InferenceResult&) = delete;
    
    // 启用移动
    InferenceResult(InferenceResult&& other) noexcept
        : imageData(other.imageData), width(other.width),
          height(other.height), channels(other.channels),
          success(other.success), errorMessage(other.errorMessage) {
        other.imageData = nullptr;
        other.success = false;
    }
    
    InferenceResult& operator=(InferenceResult&& other) noexcept {
        if (this != &other) {
            if (imageData != nullptr) {
                delete[] imageData;
            }
            imageData = other.imageData;
            width = other.width;
            height = other.height;
            channels = other.channels;
            success = other.success;
            errorMessage = other.errorMessage;
            other.imageData = nullptr;
            other.success = false;
        }
        return *this;
    }
};

/**
 * 进度回调
 */
using ProgressCallback = std::function<void(int percent, const std::string& message)>;

/**
 * 推理引擎接口
 */
class IInferenceEngine {
public:
    virtual ~IInferenceEngine() = default;
    
    /**
     * 初始化引擎
     * @param modelPath 模型路径
     * @param engineType 引擎类型
     * @return 是否初始化成功
     */
    virtual bool initialize(const std::string& modelPath, EngineType engineType) = 0;
    
    /**
     * 销毁引擎
     */
    virtual void destroy() = 0;
    
    /**
     * 文生图
     * @param prompt 提示词
     * @param negativePrompt 负面提示词
     * @param config 推理配置
     * @param callback 进度回调
     * @return 推理结果
     */
    virtual InferenceResult textToImage(
        const std::string& prompt,
        const std::string& negativePrompt,
        const InferenceConfig& config,
        ProgressCallback callback = nullptr
    ) = 0;
    
    /**
     * 图生图
     * @param inputImage 输入图像数据
     * @param inputWidth 输入宽度
     * @param inputHeight 输入高度
     * @param prompt 提示词
     * @param negativePrompt 负面提示词
     * @param config 推理配置
     * @param callback 进度回调
     * @return 推理结果
     */
    virtual InferenceResult imageToImage(
        const uint8_t* inputImage,
        int inputWidth,
        int inputHeight,
        const std::string& prompt,
        const std::string& negativePrompt,
        const InferenceConfig& config,
        ProgressCallback callback = nullptr
    ) = 0;
    
    /**
     * 局部重绘
     * @param inputImage 输入图像
     * @param mask 蒙版
     * @param prompt 提示词
     * @param negativePrompt 负面提示词
     * @param config 推理配置
     * @param callback 进度回调
     * @return 推理结果
     */
    virtual InferenceResult inpaint(
        const uint8_t* inputImage,
        const uint8_t* mask,
        int width,
        int height,
        const std::string& prompt,
        const std::string& negativePrompt,
        const InferenceConfig& config,
        ProgressCallback callback = nullptr
    ) = 0;
    
    /**
     * 超分辨率
     * @param inputImage 输入图像
     * @param inputWidth 输入宽度
     * @param inputHeight 输入高度
     * @param scale 放大倍数
     * @param callback 进度回调
     * @return 推理结果
     */
    virtual InferenceResult upscale(
        const uint8_t* inputImage,
        int inputWidth,
        int inputHeight,
        int scale,
        ProgressCallback callback = nullptr
    ) = 0;
    
    /**
     * 获取设备信息
     * @return 设备信息 JSON
     */
    virtual std::string getDeviceInfo() const = 0;
    
    /**
     * 获取模型信息
     * @return 模型信息 JSON
     */
    virtual std::string getModelInfo() const = 0;
    
    /**
     * 检查图像安全性
     * @param imageData 图像数据
     * @param size 数据大小
     * @return 是否安全
     */
    virtual bool checkSafety(const uint8_t* imageData, size_t size) const = 0;
    
    /**
     * 清除模型缓存
     */
    virtual void clearModelCache() = 0;
    
    /**
     * 是否已初始化
     * @return 是否已初始化
     */
    virtual bool isInitialized() const = 0;
};

/**
 * 创建推理引擎
 * @param engineType 引擎类型
 * @return 引擎指针
 */
std::unique_ptr<IInferenceEngine> createInferenceEngine(EngineType engineType);

/**
 * 获取引擎类型名称
 * @param type 引擎类型
 * @return 类型名称
 */
const char* getEngineTypeName(EngineType type);

/**
 * 获取采样器名称
 * @param sampler 采样器类型
 * @return 采样器名称
 */
const char* getSamplerName(SamplerType sampler);

} // namespace kuaihui

#endif // INFERENCE_ENGINE_H
