/*
 * Float Conversion - Float16/Float32 转换
 */

#ifndef KUAIHUI_FLOAT_CONVERSION_H
#define KUAIHUI_FLOAT_CONVERSION_H

#include <cstdint>

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

#endif // KUAIHUI_FLOAT_CONVERSION_H
