#ifndef MODEL_LOADER_H
#define MODEL_LOADER_H

#include <string>
#include <vector>
#include <memory>
#include <unordered_map>

// 前向声明 MNN 核心类
namespace MNN {
    namespace Interpreter {
        class Net;
    }
    class Tensor;
    class Session;
    class SessionConfig;
}

namespace kuaihui {

/**
 * 模型信息结构
 */
struct ModelInfo {
    std::string name;
    std::string path;
    std::string type;  // "unet", "vae", "clip", "embeddings"
    size_t size;
    bool isLoaded;
    
    ModelInfo() : size(0), isLoaded(false) {}
};

/**
 * 模型加载器 - 负责加载和管理 MNN 模型
 */
class ModelLoader {
public:
    /**
     * 构造函数
     * @param modelDir 模型目录路径
     */
    explicit ModelLoader(const std::string& modelDir);
    
    /**
     * 析构函数
     */
    ~ModelLoader();
    
    /**
     * 加载所有模型
     * @return 是否全部加载成功
     */
    bool loadAllModels();
    
    /**
     * 加载指定模型
     * @param modelName 模型名称
     * @return 是否加载成功
     */
    bool loadModel(const std::string& modelName);
    
    /**
     * 卸载指定模型
     * @param modelName 模型名称
     */
    void unloadModel(const std::string& modelName);
    
    /**
     * 卸载所有模型
     */
    void unloadAllModels();
    
    /**
     * 获取模型信息
     * @param modelName 模型名称
     * @return 模型信息
     */
    const ModelInfo* getModelInfo(const std::string& modelName) const;
    
    /**
     * 获取所有模型信息
     * @return 模型信息列表
     */
    std::vector<ModelInfo> getAllModelsInfo() const;
    
    /**
     * 检查模型是否已加载
     * @param modelName 模型名称
     * @return 是否已加载
     */
    bool isModelLoaded(const std::string& modelName) const;
    
    /**
     * 获取已加载模型数量
     * @return 已加载模型数量
     */
    int getLoadedModelCount() const;
    
    /**
     * 获取总内存使用量
     * @return 内存使用量（字节）
     */
    size_t getTotalMemoryUsage() const;
    
    /**
     * 清除模型缓存
     */
    void clearCache();
    
    /**
     * 获取错误信息
     * @return 错误信息
     */
    const std::string& getLastError() const { return lastError_; }

private:
    /**
     * 扫描模型目录
     */
    void scanModelDirectory();
    
    /**
     * 获取默认模型路径
     * @param modelType 模型类型
     * @return 模型文件路径
     */
    std::string getDefaultModelPath(const std::string& modelType) const;
    
    /**
     * 验证模型文件
     * @param path 模型文件路径
     * @return 是否有效
     */
    bool validateModelFile(const std::string& path) const;

private:
    std::string modelDir_;
    std::unordered_map<std::string, ModelInfo> models_;
    std::unordered_map<std::string, void*> modelHandles_;
    std::string lastError_;
    
    // 模型加载配置
    static const size_t MAX_CACHE_SIZE = 1024 * 1024 * 1024; // 1GB
    static const int DEFAULT_THREAD_COUNT = 4;
};

/**
 * LoRA 管理器
 */
class LoraManager {
public:
    LoraManager();
    ~LoraManager();
    
    /**
     * 加载 LoRA 模型
     * @param loraPath LoRA 文件路径
     * @param strength 强度
     * @return 是否加载成功
     */
    bool loadLora(const std::string& loraPath, float strength);
    
    /**
     * 卸载 LoRA
     * @param loraPath LoRA 文件路径
     */
    void unloadLora(const std::string& loraPath);
    
    /**
     * 卸载所有 LoRA
     */
    void unloadAll();
    
    /**
     * 获取已加载的 LoRA 列表
     * @return LoRA 路径列表
     */
    std::vector<std::string> getLoadedLoras() const;
    
    /**
     * 获取 LoRA 强度
     * @param loraPath LoRA 文件路径
     * @return 强度值
     */
    float getLoraStrength(const std::string& loraPath) const;

private:
    std::unordered_map<std::string, float> loadedLoras_;
    std::unordered_map<std::string, void*> loraHandles_;
};

/**
 * Embeddings 管理器
 */
class EmbeddingsManager {
public:
    EmbeddingsManager();
    ~EmbeddingsManager();
    
    /**
     * 加载 Embeddings
     * @param embeddingPath Embeddings 文件路径
     * @return 是否加载成功
     */
    bool loadEmbedding(const std::string& embeddingPath);
    
    /**
     * 卸载 Embeddings
     * @param embeddingPath Embeddings 文件路径
     */
    void unloadEmbedding(const std::string& embeddingPath);
    
    /**
     * 卸载所有 Embeddings
     */
    void unloadAll();
    
    /**
     * 获取已加载的 Embeddings 列表
     * @return Embeddings 路径列表
     */
    std::vector<std::string> getLoadedEmbeddings() const;

private:
    std::unordered_set<std::string> loadedEmbeddings_;
    std::unordered_map<std::string, void*> embeddingHandles_;
};

} // namespace kuaihui

#endif // MODEL_LOADER_H
