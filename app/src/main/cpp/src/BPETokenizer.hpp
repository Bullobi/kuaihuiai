/*
 * BPETokenizer - GPT-2/SD 兼容的分词器
 * 
 * Stable Diffusion 使用 GPT-2 的 BPE 词汇表（50257 tokens）
 * 词汇表来自 openai/clip，引用于：
 * https://github.com/openai/CLIP/blob/main/clip/simple_tokenizer.py
 * 
 * 本实现包含完整词汇表 + 字节级 BPE 合并算法
 * 相比原始随机分词，这能正确处理中文、特殊字符、多语言文本
 */

#ifndef KUAIHUI_BPE_TOKENIZER_H
#define KUAIHUI_BPE_TOKENIZER_H

#include <string>
#include <vector>
#include <unordered_map>
#include <unordered_set>

namespace kuaihui {

struct BPEToken {
    int id;              // token ID (0-50256)
    std::string text;    // token 文本
    float logProbability; // 对数概率（用于加权提示词）
    
    BPEToken() : id(0), text(""), logProbability(0.0f) {}
    BPEToken(int i, const std::string& t, float p = 0.0f)
        : id(i), text(t), logProbability(p) {}
};

// 词汇表大小
constexpr int VOCAB_SIZE = 50257;
constexpr int MAX_SEQ_LEN = 77;  // SD 默认最大 token 长度

class BPETokenizer {
public:
    BPETokenizer();
    ~BPETokenizer() = default;

    /**
     * 对文本进行分词
     * @param text 输入文本（支持 UTF-8 多语言）
     * @param maxLength 最大输出 token 数（默认 77）
     * @return token ID 数组（已 padding 到 maxLength）
     */
    std::vector<int> encode(const std::string& text, int maxLength = MAX_SEQ_LEN);

    /**
     * 批量编码（用于正负提示词）
     */
    void encodeBatch(const std::string& posPrompt,
                     const std::string& negPrompt,
                     std::vector<int>& outPosTokens,
                     std::vector<int>& outNegTokens,
                     int maxLength = MAX_SEQ_LEN);

    /**
     * 解码 token 序列为文本
     */
    std::string decode(const std::vector<int>& tokenIds);

    /**
     * 获取词汇表大小
     */
    int vocabSize() const { return VOCAB_SIZE; }

private:
    // 字节级词汇表（GPT-2 特殊设计）
    std::vector<std::string> mByteVocab;

    // BPE 合并规则表
    std::vector<std::pair<std::string, std::string>> mMerges;

    // 完整词汇表
    std::unordered_map<std::string, int> mVocab;

    // 初始化词汇表
    void initByteVocab();
    void initMerges();
    void buildVocab();

    // BPE 合并算法
    std::vector<std::string> getPairs(const std::vector<std::string>& chars);
    std::string applyBPE(const std::vector<std::string>& tokens);

    // UTF-8 字节化
    std::vector<std::string> utf8ToBytes(const std::string& text);

    // 特殊 token
    static constexpr int PAD_TOKEN = 0;       // <|pad|>
    static constexpr int BOS_TOKEN = 1;       // <|start|>
    static constexpr int EOS_TOKEN = 2;       // <|end|>
    static constexpr int UNK_TOKEN = 3;       // <|unk|>
    static constexpr int MASK_TOKEN = 4;      // <|mask|>
};

} // namespace kuaihui

#endif // KUAIHUI_BPE_TOKENIZER_H
