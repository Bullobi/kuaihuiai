/*
 * VAEDecoder.cpp - Variational Autoencoder Decoder
 * For converting latent representations to images
 */

#include <cstring>
#include <cmath>
#include <vector>
#include <fstream>
#include <memory>
#include <MNN/Interpreter.hpp>
#include <MNN/Tensor.hpp>
#include <android/log.h>

#define LOG_TAG "VAEDecoder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace kuaihui {

/**
 * VAE Decoder
 * Converts latent space representations back to RGB images
 */
class VAEDecoderImpl {
public:
    VAEDecoderImpl() : interpreter(nullptr), session(nullptr) {}
    
    ~VAEDecoderImpl() {
        unload();
    }
    
    /**
     * Load VAE decoder model
     */
    bool loadModel(const std::string& modelPath) {
        if (modelPath.empty() || !fileExists(modelPath)) {
            LOGE("VAE model path is invalid or file doesn't exist");
            return false;
        }
        
        LOGI("Loading VAE decoder from: %s", modelPath.c_str());
        
        interpreter.reset(MNN::Interpreter::createFromFile(modelPath.c_str()));
        if (!interpreter) {
            LOGE("Failed to create VAE interpreter");
            return false;
        }
        
        // Configure for CPU inference
        MNN::ScheduleConfig config;
        config.type = MNN_FORWARD_CPU;
        config.numThread = 4;
        
        // Create session
        session = interpreter->createSession(config);
        if (!session) {
            LOGE("Failed to create VAE session");
            return false;
        }
        
        // Get input and output tensors
        inputTensor = interpreter->getSessionInput(session, nullptr);
        outputTensor = interpreter->getSessionOutput(session, nullptr);
        
        if (!inputTensor || !outputTensor) {
            LOGE("Failed to get VAE tensors");
            return false;
        }
        
        LOGI("VAE decoder loaded successfully");
        return true;
    }
    
    /**
     * Unload model
     */
    void unload() {
        if (session) {
            interpreter->releaseSession(session);
            session = nullptr;
        }
        inputTensor = nullptr;
        outputTensor = nullptr;
        interpreter.reset();
    }
    
    /**
     * Decode latent to image
     * @param latent Input latent tensor (N, 4, H, W)
     * @param latentW Latent width
     * @param latentH Latent height
     * @param pixels Output pixel buffer (RGBA format)
     * @param pixelW Output image width
     * @param pixelH Output image height
     */
    void decode(const float* latent, int latentW, int latentH, 
                float* pixels, int pixelW, int pixelH) {
        
        if (!session || !inputTensor) {
            // Fallback: simple upsampling with color transform
            fallbackDecode(latent, latentW, latentH, pixels, pixelW, pixelH);
            return;
        }
        
        // Copy latent data to input tensor
        auto inputShape = inputTensor->shape();
        if (inputShape.size() >= 4) {
            // NCHW format
            memcpy(inputTensor->host<float>(), latent, 
                   latentW * latentH * 4 * sizeof(float));
            
            // Run inference
            interpreter->runSession(session);
            
            // Get output
            memcpy(pixels, outputTensor->host<float>(), 
                   pixelW * pixelH * 4 * sizeof(float));
        } else {
            fallbackDecode(latent, latentW, latentH, pixels, pixelW, pixelH);
        }
    }
    
    /**
     * Simple fallback decoder when MNN model is not available
     */
    void fallbackDecode(const float* latent, int latentW, int latentH,
                       float* pixels, int pixelW, int pixelH) {
        int scale = pixelW / latentW;  // Usually 8 for SD
        
        for (int y = 0; y < pixelH; y++) {
            for (int x = 0; x < pixelW; x++) {
                int lx = std::min(x / scale, latentW - 1);
                int ly = std::min(y / scale, latentH - 1);
                int latentIdx = (ly * latentW + lx) * 4;
                int pixelIdx = (y * pixelW + x) * 4;
                
                // Convert from [-1, 1] to [0, 1] and apply color grading
                float r = latent[latentIdx];
                float g = latent[latentIdx + 1];
                float b = latent[latentIdx + 2];
                float a = 1.0f;
                
                // Simple color correction
                r = std::tanh(r * 0.8f) * 0.5f + 0.5f;
                g = std::tanh(g * 0.8f) * 0.5f + 0.5f;
                b = std::tanh(b * 0.8f) * 0.5f + 0.5f;
                
                pixels[pixelIdx] = r;
                pixels[pixelIdx + 1] = g;
                pixels[pixelIdx + 2] = b;
                pixels[pixelIdx + 3] = a;
            }
        }
    }
    
    bool isLoaded() const { return session != nullptr; }
    
private:
    std::unique_ptr<MNN::Interpreter> interpreter;
    MNN::Session* session = nullptr;
    MNN::Tensor* inputTensor = nullptr;
    MNN::Tensor* outputTensor = nullptr;
    
    bool fileExists(const std::string& path) {
        std::ifstream f(path.c_str());
        return f.good();
    }
};

// Global instance
static VAEDecoderImpl* g_vaeDecoder = nullptr;

bool loadVAEDecoder(const std::string& modelPath) {
    if (!g_vaeDecoder) {
        g_vaeDecoder = new VAEDecoderImpl();
    }
    return g_vaeDecoder->loadModel(modelPath);
}

void unloadVAEDecoder() {
    if (g_vaeDecoder) {
        delete g_vaeDecoder;
        g_vaeDecoder = nullptr;
    }
}

void decodeLatent(const float* latent, int latentW, int latentH,
                  float* pixels, int pixelW, int pixelH) {
    if (g_vaeDecoder) {
        g_vaeDecoder->decode(latent, latentW, latentH, pixels, pixelW, pixelH);
    }
}

bool isVAEDecoderLoaded() {
    return g_vaeDecoder && g_vaeDecoder->isLoaded();
}

} // namespace kuaihui
