/*
 * ProgressReporter.cpp - Simplified Progress Callback System
 */

#include "ProgressReporter.hpp"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "ProgressReporter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace kuaihui {

void ProgressReporter::setJNIEnv(JNIEnv* env, jobject callbackObj) {
    if (!callbackObj) {
        LOGE("Progress callback is null");
        return;
    }
    
    if (env->GetJavaVM(&mJvm) != JNI_OK) {
        LOGE("Failed to get JavaVM");
        return;
    }
    
    mCallbackObj = env->NewGlobalRef(callbackObj);
    
    jclass cls = env->GetObjectClass(callbackObj);
    mReportMethod = env->GetMethodID(cls, "onProgress", "(ILjava/lang/String;)V");
    mErrorMethod = env->GetMethodID(cls, "onError", "(Ljava/lang/String;)V");
    mCompletedMethod = env->GetMethodID(cls, "onComplete", "()V");
    env->DeleteLocalRef(cls);
    
    LOGI("Progress reporter initialized");
}

void ProgressReporter::report(const ProgressInfo& info) {
    if (!mCallbackObj || !mJvm || !mReportMethod) return;
    
    ensureJNIThread();
    
    JNIEnv* env = nullptr;
    if (mJvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return;
    
    jstring jmsg = env->NewStringUTF(info.message.c_str());
    env->CallVoidMethod(mCallbackObj, mReportMethod, info.percent, jmsg);
    env->DeleteLocalRef(jmsg);
}

void ProgressReporter::reportStage(ProgressStage stage, int step, int totalSteps,
                                   const std::string& msg, float latentSigma) {
    ProgressInfo info;
    info.stage = stage;
    info.step = step;
    info.totalSteps = totalSteps;
    info.percent = (totalSteps > 0) ? (step * 100 / totalSteps) : 0;
    info.latentStep = latentSigma;
    info.message = msg;
    report(info);
}

void ProgressReporter::reportError(const std::string& errorMsg) {
    if (!mCallbackObj || !mJvm || !mErrorMethod) return;
    
    ensureJNIThread();
    
    JNIEnv* env = nullptr;
    if (mJvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return;
    
    jstring jmsg = env->NewStringUTF(errorMsg.c_str());
    env->CallVoidMethod(mCallbackObj, mErrorMethod, jmsg);
    env->DeleteLocalRef(jmsg);
}

void ProgressReporter::reportCompleted() {
    if (!mCallbackObj || !mJvm || !mCompletedMethod) return;
    
    ensureJNIThread();
    
    JNIEnv* env = nullptr;
    if (mJvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return;
    
    env->CallVoidMethod(mCallbackObj, mCompletedMethod);
}

void ProgressReporter::ensureJNIThread() {
    if (!mJvm) return;
    
    JNIEnv* env = nullptr;
    int status = mJvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    
    if (status == JNI_EDETACHED) {
        mJvm->AttachCurrentThread(&env, nullptr);
    }
}

} // namespace kuaihui
