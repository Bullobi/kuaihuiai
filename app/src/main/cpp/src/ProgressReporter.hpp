/*
 * ProgressReporter - JNI 进度回调系统
 * 通过 JNI 全局引用将推理进度实时推送回 Kotlin 层
 */

#ifndef KUAIHUI_PROGRESS_REPORTER_H
#define KUAIHUI_PROGRESS_REPORTER_H

#include <jni.h>
#include <functional>
#include <string>

namespace kuaihui {

enum class ProgressStage {
    INITIALIZING,
    TOKENIZING,
    ENCODING_PROMPT,
    CREATING_NOISE,
    DENOISING,
    DECODING_LATENT,
    ENCODING_IMAGE,
    SAVING,
    COMPLETED,
    ERROR
};

struct ProgressInfo {
    ProgressStage stage;
    int step;           // 当前步数 (1-based)
    int totalSteps;     // 总步数
    int percent;        // 0-100
    float latentStep;   // 当前 latent 步骤的 sigma 值
    std::string message;
    std::string stageName;
};

class ProgressReporter {
public:
    using ProgressCallback = std::function<void(const ProgressInfo&)>;

    ProgressReporter() = default;
    ~ProgressReporter() = default;

    // 设置 JNI 回调（从 Kotlin 层调用）
    void setJNIEnv(JNIEnv* env, jobject callbackObj);

    // 报告进度（所有线程安全）
    void report(const ProgressInfo& info);

    // 便捷方法
    void reportStage(ProgressStage stage, int step, int totalSteps,
                     const std::string& msg, float latentSigma = 0.0f);

    void reportError(const std::string& errorMsg);
    void reportCompleted();

    // 查询是否已取消
    bool isCancelled() const { return mCancelled; }
    void cancel() { mCancelled = true; }

private:
    JavaVM* mJvm = nullptr;
    jobject mCallbackObj = nullptr;
    jmethodID mReportMethod = nullptr;
    jmethodID mErrorMethod = nullptr;
    jmethodID mCompletedMethod = nullptr;
    std::atomic<bool> mCancelled{false};

    void ensureJNIThread();
};

} // namespace kuaihui

#endif // KUAIHUI_PROGRESS_REPORTER_H
