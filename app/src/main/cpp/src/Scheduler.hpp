// Scheduler interface for diffusion models
#ifndef SCHEDULER_HPP
#define SCHEDULER_HPP

#include <optional>
#include <string>
#include <vector>

// Simple tensor-like structure using std::vector
struct Tensor {
    std::vector<float> data;
    std::vector<int> shape;
    
    Tensor() {}
    
    Tensor(const std::vector<int>& s) : shape(s) {
        int size = 1;
        for (int d : s) size *= d;
        data.resize(size, 0.0f);
    }
    
    float& at(int i) { return data[i]; }
    const float& at(int i) const { return data[i]; }
    
    int size() const { return data.size(); }
    int flatIndex(const std::vector<int>& indices) const {
        int idx = 0;
        int stride = 1;
        for (int i = shape.size() - 1; i >= 0; i--) {
            idx += indices[i] * stride;
            stride *= shape[i];
        }
        return idx;
    }
};

class Scheduler {
public:
    struct SchedulerOutput {
        Tensor prev_sample;
        Tensor pred_original_sample;
    };

    virtual ~Scheduler() = default;

    virtual void set_timesteps(int num_inference_steps) = 0;
    virtual Tensor scale_model_input(const Tensor& sample, int timestep) = 0;
    virtual SchedulerOutput step(const Tensor& model_output, int timestep, 
                                  const Tensor& sample) = 0;
    virtual Tensor add_noise(const Tensor& original_samples, 
                            const Tensor& noise,
                            const std::vector<int>& timesteps) const = 0;
    virtual void set_begin_index(int begin_index) = 0;
    virtual void set_prediction_type(const std::string& prediction_type) = 0;
    virtual const std::vector<float>& get_timesteps() const = 0;
    virtual size_t get_step_index() const = 0;
    virtual float get_current_sigma() const = 0;
    virtual float get_init_noise_sigma() const = 0;
};

#endif // SCHEDULER_HPP
