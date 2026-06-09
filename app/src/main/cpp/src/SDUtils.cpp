/*
 * SD Utils - Stable Diffusion 工具函数
 */

#include <cmath>
#include <vector>
#include <string>

// Float 转换函数
inline float fp32_to_fp16(float f) {
    unsigned int ui = *reinterpret_cast<unsigned int*>(&f);
    unsigned int sign = (ui >> 16) & 0x8000;
    unsigned int exponent = ((ui >> 23) - 127 + 15) & 0x1F;
    unsigned int mantissa = (ui >> 13) & 0x3FF;
    return (float)(sign | (exponent << 10) | mantissa);
}

inline float fp16_to_fp32(uint16_t h) {
    unsigned int sign = (h >> 15) & 0x1;
    unsigned int exponent = (h >> 10) & 0x1F;
    unsigned int mantissa = h & 0x3FF;
    
    unsigned int ui;
    if (exponent == 0) {
        if (mantissa == 0) {
            ui = sign << 31;
        } else {
            // Denormal
            int e = -1;
            do {
                e++;
                mantissa <<= 1;
            } while ((mantissa & 0x400) == 0);
            mantissa &= 0x3FF;
            ui = (sign << 31) | ((-e - 1 + 127) << 23) | (mantissa << 13);
        }
    } else if (exponent == 31) {
        ui = (sign << 31) | (0xFF << 23) | (mantissa << 13);
    } else {
        ui = (sign << 31) | ((exponent - 15 + 127) << 23) | (mantissa << 13);
    }
    
    return *reinterpret_cast<float*>(&ui);
}

// Scheduler 相关函数
namespace SchedulerUtils {

// Euler Scheduler 步进
inline void eulerStep(std::vector<float>& latents, const std::vector<float>& noisePred, 
                      float dt) {
    for (size_t i = 0; i < latents.size(); i++) {
        latents[i] += noisePred[i] * dt;
    }
}

// Euler Ancestral Scheduler 步进（添加随机性）
inline void eulerAncestralStep(std::vector<float>& latents, const std::vector<float>& noisePred,
                               const std::vector<float>& randomNoise, float dt, float sigma) {
    for (size_t i = 0; i < latents.size(); i++) {
        latents[i] += noisePred[i] * dt;
        latents[i] += randomNoise[i] * sigma;
    }
}

// DPM-Solver 2M 步进
inline void dpmSolver2MStep(std::vector<float>& latents, 
                            const std::vector<float>& pred1,
                            const std::vector<float>& pred2,
                            float dt) {
    // 2阶 DPM-Solver
    for (size_t i = 0; i < latents.size(); i++) {
        latents[i] += (1.5f * pred1[i] - 0.5f * pred2[i]) * dt;
    }
}

// 计算 sigma 值
inline float getSigma(int step, int totalSteps, float sigmaMin = 0.0292f, float sigmaMax = 14.6146f) {
    float t = (float)step / totalSteps;
    // 线性调度
    return sigmaMin + (sigmaMax - sigmaMin) * t;
}

// Karmas 调度
inline float getSigmaKarras(int step, int totalSteps, float sigmaMin = 0.0292f, float sigmaMax = 14.6146f) {
    float t = (float)step / totalSteps;
    float ramp = t * t * (3 - 2 * t); // smoothstep
    float sigma = sigmaMin * std::pow(sigmaMax / sigmaMin, ramp);
    return sigma;
}

} // namespace SchedulerUtils

// 图像处理
namespace ImageUtils {

// 将浮点图像数据转换为字节
inline void floatToUint8(const std::vector<float>& input, std::vector<uint8_t>& output) {
    output.resize(input.size());
    for (size_t i = 0; i < input.size(); i++) {
        float val = std::max(0.0f, std::min(1.0f, (input[i] + 1.0f) * 0.5f));
        output[i] = (uint8_t)(val * 255.0f);
    }
}

// 将字节图像数据转换为浮点
inline void uint8ToFloat(const std::vector<uint8_t>& input, std::vector<float>& output) {
    output.resize(input.size());
    for (size_t i = 0; i < input.size(); i++) {
        output[i] = (float)input[i] / 255.0f * 2.0f - 1.0f;
    }
}

// 调整图像大小（最近邻）
inline void resizeImage(const uint8_t* input, int inputW, int inputH,
                       uint8_t* output, int outputW, int outputH) {
    float scaleX = (float)inputW / outputW;
    float scaleY = (float)inputH / outputH;
    
    for (int y = 0; y < outputH; y++) {
        for (int x = 0; x < outputW; x++) {
            int srcX = (int)(x * scaleX);
            int srcY = (int)(y * scaleY);
            srcX = std::min(srcX, inputW - 1);
            srcY = std::min(srcY, inputH - 1);
            
            int srcIdx = (srcY * inputW + srcX) * 3;
            int dstIdx = (y * outputW + x) * 3;
            
            output[dstIdx + 0] = input[srcIdx + 0];
            output[dstIdx + 1] = input[srcIdx + 1];
            output[dstIdx + 2] = input[srcIdx + 2];
        }
    }
}

} // namespace ImageUtils
