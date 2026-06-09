/*
 * Prompt Processor 头文件
 */

#ifndef KUAIHUI_PROMPT_PROCESSOR_H
#define KUAIHUI_PROMPT_PROCESSOR_H

#include <string>
#include <vector>

struct PromptToken {
    std::string text;
    float weight;
    bool is_embedding;
    std::vector<float> embedding_data;
    std::vector<float> embedding_data_2;
    
    PromptToken(const std::string& t = "", float w = 1.0f) 
        : text(t), weight(w), is_embedding(false) {}
};

class PromptProcessor {
public:
    std::vector<PromptToken> process(const std::string& prompt);
    std::string decode(const std::vector<int>& tokenIds);
};

#endif // KUAIHUI_PROMPT_PROCESSOR_H
