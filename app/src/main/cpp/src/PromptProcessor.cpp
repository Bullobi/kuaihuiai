/*
 * Prompt Processor - 提示词处理器
 */

#include <string>
#include <vector>
#include <regex>

struct PromptToken {
    std::string text;
    float weight;
    bool is_embedding;
    std::vector<float> embedding_data;
    std::vector<float> embedding_data_2;
    
    PromptToken(const std::string& t, float w = 1.0f) 
        : text(t), weight(w), is_embedding(false) {}
};

class PromptProcessor {
public:
    std::vector<PromptToken> process(const std::string& prompt) {
        std::vector<PromptToken> tokens;
        
        // 解析加权提示词语法 (tag:weight)
        std::regex weightRegex(R"(([^:]+)(?::(\d+(?:\.\d+)?))?)");
        std::sregex_iterator it(prompt.begin(), prompt.end(), weightRegex);
        std::sregex_iterator end;
        
        while (it != end) {
            std::smatch match = *it;
            std::string text = match[1].str();
            float weight = match[2].matched ? std::stof(match[2].str()) : 1.0f;
            
            tokens.emplace_back(text, weight);
            ++it;
        }
        
        // 如果没有匹配，返回原始文本
        if (tokens.empty()) {
            tokens.emplace_back(prompt, 1.0f);
        }
        
        return tokens;
    }
    
    std::string decode(const std::vector<int>& tokenIds) {
        // 简化的解码
        std::string result;
        for (int id : tokenIds) {
            if (id > 0 && id < 256) {
                result += (char)id;
            }
        }
        return result;
    }
};
