/**
 * Tokenizer - Text to Token Conversion
 */

#include <string>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "Tokenizer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class Tokenizer {
public:
    Tokenizer();
    ~Tokenizer();
    
    // Initialize with vocabulary
    bool initialize(const std::string& vocabPath);
    
    // Tokenize text
    std::vector<int> encode(const std::string& text);
    
    // Detokenize
    std::string decode(const std::vector<int>& tokens);
    
    // Get vocabulary size
    int getVocabSize() const { return mVocabSize; }
    
private:
    int mVocabSize = 49408; // Default for SD
    std::unordered_map<std::string, int> mWordToId;
    std::unordered_map<int, std::string> mIdToWord;
    
    // Pre-defined vocabulary (simplified)
    void loadDefaultVocab();
    
    // Tokenize with BPE
    std::vector<std::string> tokenizeBPE(const std::string& text);
};

// Simple default vocabulary (most common tokens)
static const char* DEFAULT_VOCAB[] = {
    "a", "the", "of", "and", "in", "to", "is", "it", "an", "as",
    // ... this would be a full vocabulary in production
};

Tokenizer::Tokenizer() {
    LOGI("Tokenizer created");
    loadDefaultVocab();
}

Tokenizer::~Tokenizer() {
    LOGI("Tokenizer destroyed");
}

void Tokenizer::loadDefaultVocab() {
    // In production, this would load the actual vocabulary file
    // For now, we initialize with basic tokens
    
    // Common English words (simplified)
    std::vector<std::string> commonWords = {
        "a", "an", "the", "be", "have", "do", "say", "get", "make", "go",
        "know", "take", "see", "come", "think", "look", "want", "use", "find", "give"
    };
    
    for (size_t i = 0; i < commonWords.size(); ++i) {
        mWordToId[commonWords[i]] = static_cast<int>(i);
        mIdToWord[static_cast<int>(i)] = commonWords[i];
    }
    
    // Special tokens
    mWordToId["<PAD>"] = 0;
    mWordToId["<UNK>"] = 1;
    mWordToId["<START>"] = 2;
    mWordToId["<END>"] = 3;
    mWordToId["<MASK>"] = 4;
}

bool Tokenizer::initialize(const std::string& vocabPath) {
    LOGI("Initializing tokenizer with vocab: %s", vocabPath.c_str());
    // Load vocabulary from file
    return true;
}

std::vector<int> Tokenizer::encode(const std::string& text) {
    std::vector<int> tokens;
    
    // Convert to lowercase and split
    std::string lowerText = text;
    std::transform(lowerText.begin(), lowerText.end(), lowerText.begin(), ::tolower);
    
    // Simple word-based tokenization
    std::string word;
    for (char c : lowerText) {
        if (c == ' ' || c == ',' || c == '.' || c == '!') {
            if (!word.empty()) {
                if (mWordToId.count(word)) {
                    tokens.push_back(mWordToId[word]);
                } else {
                    tokens.push_back(mWordToId["<UNK>"]);
                }
                word.clear();
            }
        } else {
            word += c;
        }
    }
    
    // Handle last word
    if (!word.empty()) {
        if (mWordToId.count(word)) {
            tokens.push_back(mWordToId[word]);
        } else {
            tokens.push_back(mWordToId["<UNK>"]);
        }
    }
    
    // Pad to maximum length (77 for SD)
    while (tokens.size() < 77) {
        tokens.push_back(mWordToId["<PAD>"]);
    }
    
    // Truncate if too long
    if (tokens.size() > 77) {
        tokens.resize(77);
    }
    
    return tokens;
}

std::string Tokenizer::decode(const std::vector<int>& tokens) {
    std::string text;
    
    for (int token : tokens) {
        if (token == mWordToId["<PAD>"] || 
            token == mWordToId["<END>"]) {
            break;
        }
        
        if (mIdToWord.count(token)) {
            text += mIdToWord[token] + " ";
        }
    }
    
    return text;
}
