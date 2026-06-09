/**
 * JNI 桥接 - 空实现，用于链接
 * 实际功能在 native_engine.cpp 中实现
 */

// 这个文件确保 CMake 可以正确链接
// 实际的 JNI 函数在各自的源文件中实现

#include <jni.h>

// 占位符，确保库可以正确加载
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    return JNI_VERSION_1_6;
}
