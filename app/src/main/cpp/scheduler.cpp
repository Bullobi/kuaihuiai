/**
 * Scheduler Implementations
 */

#include <vector>
#include <cmath>

namespace Schedulers {

// DDIM Scheduler
std::vector<float> ddim(std::vector<float> latent, float eta, int step) {
    // DDIM (Denoising Diffusion Implicit Models) scheduler
    //eta = 0 means deterministic, eta = 1 means stochastic
    
    float alpha_t = 1.0f - (step / 100.0f);
    float alpha_t_prev = 1.0f - ((step + 1) / 100.0f);
    
    for (auto& val : latent) {
        val = val * std::sqrt(alpha_t_prev / alpha_t);
    }
    
    return latent;
}

// Euler Scheduler (simple)
std::vector<float> euler(std::vector<float> latent, float step, float totalSteps) {
    float t = step / totalSteps;
    float sigma = std::sqrt(t);
    
    for (auto& val : latent) {
        val = val * (1.0f - sigma * 0.5f);
    }
    
    return latent;
}

// DPMSolver (DPM-Solver++)
std::vector<float> dpmSolver(std::vector<float> latent, int step, int totalSteps) {
    // DPM-Solver is a fast ODE solver for diffusion models
    float lambda = static_cast<float>(step) / static_cast<float>(totalSteps);
    float coef = 1.0f / (1.0f + lambda);
    
    for (auto& val : latent) {
        val *= coef;
    }
    
    return latent;
}

} // namespace Schedulers
