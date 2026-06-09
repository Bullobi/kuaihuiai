/*
 * SD Utils 头文件
 */

#ifndef KUAIHUI_SD_UTILS_H
#define KUAIHUI_SD_UTILS_H

#include <cmath>
#include <vector>
#include <cstdint>

// Float 转换
float fp32_to_fp16(float f);
float fp16_to_fp32(uint16_t h);

// Scheduler 工具
namespace SchedulerUtils {
    void eulerStep(std::vector<float>& latents, const std::vector<float>& noisePred, float dt);
    void eulerAncestralStep(std::vector<float>& latents, const std::vector<float>& noisePred,
                           const std::vector<float>& randomNoise, float dt, float sigma);
    void dpmSolver2MStep(std::vector<float>& latents, const std::vector<float>& pred1,
                         const std::vector<float>& pred2, float dt);
    float getSigma(int step, int totalSteps, float sigmaMin = 0.0292f, float sigmaMax = 14.6146f);
    float getSigmaKarras(int step, int totalSteps, float sigmaMin = 0.0292f, float sigmaMax = 14.6146f);
}

// 图像工具
namespace ImageUtils {
    void floatToUint8(const std::vector<float>& input, std::vector<uint8_t>& output);
    void uint8ToFloat(const std::vector<uint8_t>& input, std::vector<float>& output);
    void resizeImage(const uint8_t* input, int inputW, int inputH,
                    uint8_t* output, int outputW, int outputH);
}

#endif // KUAIHUI_SD_UTILS_H
