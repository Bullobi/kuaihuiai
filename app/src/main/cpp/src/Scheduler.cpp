/*
 * Scheduler.cpp - Diffusion Schedulers
 * Implements various sampling schedules for DDIM, Euler, DPM-Solver, etc.
 */

#include <cmath>
#include <vector>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "Scheduler"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace kuaihui {

// Scheduler types
enum class SchedulerType {
    EULER = 0,
    EULER_ANCESTRAL = 1,
    DDIM = 2,
    PNDM = 3,
    DPM_2M = 4,
    DPM_2M_KARRAS = 5,
    DPM_SDE = 6,
    DPM_SDE_KARRAS = 7,
    LCM = 8,
};

/**
 * Diffusion Scheduler
 * Manages the denoising schedule
 */
class SchedulerImpl {
public:
    SchedulerImpl() {
        // Default values for Stable Diffusion
        betaStart = 0.00085f;
        betaEnd = 0.012f;
        numTrainTimesteps = 1000;
        numInferenceSteps = 20;
        
        // Precompute schedule
        computeSchedules();
    }
    
    ~SchedulerImpl() = default;
    
    /**
     * Set number of inference steps
     */
    void setNumInferenceSteps(int steps) {
        numInferenceSteps = steps;
        computeSchedules();
    }
    
    /**
     * Set scheduler type
     */
    void setType(SchedulerType type) {
        schedulerType = type;
    }
    
    SchedulerType getType() const { return schedulerType; }
    
    /**
     * Initialize noise sample
     */
    void addNoise(float* latent, const float* noise, int latentSize, float sigma) {
        for (int i = 0; i < latentSize; i++) {
            latent[i] = latent[i] + noise[i] * sigma;
        }
    }
    
    /**
     * Euler step
     */
    void step(float* sample, const float* modelOutput, int latentSize, float sigma, float dt) {
        float d = sigma;
        for (int i = 0; i < latentSize; i++) {
            sample[i] = sample[i] - d * modelOutput[i] * dt;
        }
    }
    
    /**
     * Euler Ancestral step (with noise)
     */
    void stepEulerAncestral(float* sample, const float* modelOutput, 
                           const float* noise, int latentSize, 
                           float sigma, float sigmaNext) {
        float sigmaDown = std::sqrt(sigmaNext * sigmaNext - sigma * sigma);
        float dt = 1.0f;
        
        for (int i = 0; i < latentSize; i++) {
            float derivative = modelOutput[i];
            sample[i] = sample[i] - derivative * sigmaDown * dt;
            if (noise) {
                sample[i] = sample[i] + noise[i] * sigmaNext * 0.1f;
            }
        }
    }
    
    /**
     * DDIM step
     */
    void stepDDIM(float* sample, const float* modelOutput, int latentSize,
                  float sigma, float sigmaNext, float eta = 0.0f) {
        // Predict x_0
        for (int i = 0; i < latentSize; i++) {
            float predOriginal = (sample[i] - std::sqrt(1.0f - sigma * sigma) * modelOutput[i]) / std::sqrt(sigma * sigma);
            
            // Direction towards pred_x_t
            float predSampleDir = std::sqrt(1.0f - sigmaNext * sigmaNext) * modelOutput[i];
            
            // Add noise if eta > 0
            float predVariance = eta * std::sqrt(sigmaNext * sigmaNext - sigma * sigma);
            
            sample[i] = std::sqrt(sigmaNext) * predOriginal + predSampleDir;
        }
    }
    
    /**
     * Get current sigma
     */
    float getSigma(int step) const {
        if (step < 0 || step >= numInferenceSteps) return 1.0f;
        return sigmas[step];
    }
    
    /**
     * Get current alpha
     */
    float getAlpha(int step) const {
        if (step < 0 || step >= numInferenceSteps) return 0.0f;
        return 1.0f - sigmas[step] * sigmas[step];
    }
    
    /**
     * Get current timestep
     */
    int getTimestep(int step) const {
        if (step < 0 || step >= numInferenceSteps) return 0;
        return timesteps[step];
    }
    
    /**
     * Get alpha_cumprod
     */
    float getAlphaCumProd(int timestep) const {
        if (timestep < 0 || timestep >= numTrainTimesteps) return 0.0f;
        return alphasCumProd[timestep];
    }
    
    /**
     * Get all sigmas
     */
    const std::vector<float>& getSigmas() const { return sigmas; }
    
    /**
     * Get all timesteps
     */
    const std::vector<int>& getTimesteps() const { return timesteps; }
    
    int getNumInferenceSteps() const { return numInferenceSteps; }
    
private:
    void computeSchedules() {
        // Compute betas
        betas.resize(numTrainTimesteps);
        for (int i = 0; i < numTrainTimesteps; i++) {
            float t = static_cast<float>(i) / numTrainTimesteps;
            betas[i] = betaStart + t * (betaEnd - betaStart);
        }
        
        // Compute alphas
        alphas.resize(numTrainTimesteps);
        for (int i = 0; i < numTrainTimesteps; i++) {
            alphas[i] = 1.0f - betas[i];
        }
        
        // Compute cumulative products
        alphasCumProd.resize(numTrainTimesteps);
        alphasCumProd[0] = alphas[0];
        for (int i = 1; i < numTrainTimesteps; i++) {
            alphasCumProd[i] = alphasCumProd[i - 1] * alphas[i];
        }
        
        // Compute inference timesteps
        timesteps.resize(numInferenceSteps);
        for (int i = 0; i < numInferenceSteps; i++) {
            timesteps[i] = static_cast<int>((1.0f - i / static_cast<float>(numInferenceSteps - 1)) * (numTrainTimesteps - 1));
        }
        
        // Compute sigmas
        sigmas.resize(numInferenceSteps);
        for (int i = 0; i < numInferenceSteps; i++) {
            sigmas[i] = std::sqrt(1.0f - alphasCumProd[timesteps[i]]);
        }
        
        // Apply scheduler-specific modifications
        switch (schedulerType) {
            case SchedulerType::DPM_2M_KARRAS:
            case SchedulerType::DPM_SDE_KARRAS:
                applyKarrasScheduling();
                break;
            default:
                break;
        }
    }
    
    void applyKarrasScheduling() {
        // Karras et al. scheduling
        std::vector<float> newSigmas(numInferenceSteps);
        float sigmaMin = sigmas.back();
        float sigmaMax = sigmas.front();
        
        for (int i = 0; i < numInferenceSteps; i++) {
            float t = static_cast<float>(i) / numInferenceSteps;
            float rho = 7.0f;  // Karras recommended
            float sigma = std::pow(sigmaMax, 1.0f / rho) + 
                         t * (std::pow(sigmaMin, 1.0f / rho) - std::pow(sigmaMax, 1.0f / rho));
            newSigmas[i] = std::pow(sigma, rho);
        }
        
        sigmas = newSigmas;
    }
    
private:
    // Beta schedule
    float betaStart;
    float betaEnd;
    int numTrainTimesteps;
    int numInferenceSteps;
    
    // Computed schedules
    std::vector<float> betas;
    std::vector<float> alphas;
    std::vector<float> alphasCumProd;
    std::vector<int> timesteps;
    std::vector<float> sigmas;
    
    // Current type
    SchedulerType schedulerType = SchedulerType::EULER;
};

// Global instance
static SchedulerImpl* g_scheduler = nullptr;

SchedulerImpl* getScheduler() {
    if (!g_scheduler) {
        g_scheduler = new SchedulerImpl();
    }
    return g_scheduler;
}

void destroyScheduler() {
    if (g_scheduler) {
        delete g_scheduler;
        g_scheduler = nullptr;
    }
}

} // namespace kuaihui
