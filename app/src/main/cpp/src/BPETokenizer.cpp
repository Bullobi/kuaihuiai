/*
 * BPETokenizer.cpp - Byte Pair Encoding Tokenizer
 * For Stable Diffusion text encoding
 */

#include <string>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <cctype>

namespace kuaihui {

/**
 * Simple BPE-like Tokenizer
 * Based on CLIP's tokenizer behavior
 */
class BPETokenizer {
public:
    BPETokenizer() {
        initializeVocab();
    }
    
    ~BPETokenizer() = default;
    
    /**
     * Tokenize text into token IDs
     * @param text Input text
     * @param maxLength Maximum sequence length
     * @return Vector of token IDs
     */
    std::vector<int> encode(const std::string& text, int maxLength = 77) {
        std::vector<int> tokens;
        tokens.reserve(maxLength);
        
        // Add start token
        tokens.push_back(49406);  // BOS token
        
        // Tokenize characters and common patterns
        size_t i = 0;
        while (i < text.length() && tokens.size() < maxLength - 1) {
            // Try to match common 2-3 char patterns first
            if (i + 1 < text.length()) {
                int pairToken = lookupPair(text.substr(i, 2));
                if (pairToken >= 0) {
                    tokens.push_back(pairToken);
                    i += 2;
                    continue;
                }
            }
            
            if (i + 2 < text.length()) {
                int tripleToken = lookupTriple(text.substr(i, 3));
                if (tripleToken >= 0) {
                    tokens.push_back(tripleToken);
                    i += 3;
                    continue;
                }
            }
            
            // Single character
            unsigned char c = static_cast<unsigned char>(text[i]);
            
            if (c < 128) {
                // ASCII
                if (std::isspace(c)) {
                    tokens.push_back(49407);  // PAD token
                } else {
                    tokens.push_back(c + 256);  // Offset for ASCII
                }
            } else {
                // UTF-8 multi-byte
                int codepoint = decodeUTF8(text, i);
                if (codepoint > 0) {
                    tokens.push_back(std::min(codepoint, 49405));  // CLIP vocab size
                    i += getUTF8Length(c);
                } else {
                    i++;
                }
            }
        }
        
        // Add end token
        tokens.push_back(49406);  // EOS token
        
        // Pad to maxLength
        while (tokens.size() < maxLength) {
            tokens.push_back(49407);  // PAD token
        }
        
        // Truncate if necessary
        if (tokens.size() > maxLength) {
            tokens.resize(maxLength);
        }
        
        return tokens;
    }
    
    /**
     * Decode tokens back to text
     * @param tokens Token IDs
     * @return Decoded text
     */
    std::string decode(const std::vector<int>& tokens) {
        std::string text;
        
        for (size_t i = 0; i < tokens.size(); i++) {
            int token = tokens[i];
            
            // Skip special tokens
            if (token == 49406 || token == 49407 || token == 0) {
                continue;
            }
            
            if (token >= 256 && token < 49406) {
                // Character token
                text += static_cast<char>(token - 256);
            } else if (token >= 49408) {
                // Special token (ignore)
            } else {
                // Unicode codepoint
                text += codepointToUTF8(token);
            }
        }
        
        return text;
    }
    
    /**
     * Get vocabulary size
     */
    int vocabSize() const { return 49408; }
    
private:
    std::unordered_map<std::string, int> pairVocab;
    
    void initializeVocab() {
        // Build common pair mappings
        // In a real implementation, this would load from tokenizer.json
        // For now, we use a simple character-based approach
        
        // Common pairs (simplified)
        pairVocab["th"] = 400;
        pairVocab["he"] = 401;
        pairVocab["in"] = 402;
        pairVocab["er"] = 403;
        pairVocab["on"] = 404;
        pairVocab["an"] = 405;
        pairVocab["re"] = 406;
        pairVocab["ed"] = 407;
        pairVocab["nd"] = 408;
        pairVocab["at"] = 409;
        pairVocab["or"] = 410;
        pairVocab["te"] = 411;
        pairVocab["en"] = 412;
        pairVocab["ti"] = 413;
        pairVocab["es"] = 414;
        pairVocab["ar"] = 415;
        pairVocab["se"] = 416;
        pairVocab["ea"] = 417;
        pairVocab["le"] = 418;
        pairVocab["ne"] = 419;
    }
    
    int lookupPair(const std::string& pair) {
        auto it = pairVocab.find(pair);
        return (it != pairVocab.end()) ? it->second : -1;
    }
    
    int lookupTriple(const std::string& triple) {
        // Simplified: no triple lookup
        return -1;
    }
    
    int decodeUTF8(const std::string& text, size_t& i) {
        unsigned char c = static_cast<unsigned char>(text[i]);
        
        if ((c & 0x80) == 0) {
            return c;
        } else if ((c & 0xE0) == 0xC0 && i + 1 < text.length()) {
            return ((c & 0x1F) << 6) | (static_cast<unsigned char>(text[i + 1]) & 0x3F);
        } else if ((c & 0xF0) == 0xE0 && i + 2 < text.length()) {
            return ((c & 0x0F) << 12) | 
                   ((static_cast<unsigned char>(text[i + 1]) & 0x3F) << 6) |
                   (static_cast<unsigned char>(text[i + 2]) & 0x3F);
        } else if ((c & 0xF8) == 0xF0 && i + 3 < text.length()) {
            return ((c & 0x07) << 18) |
                   ((static_cast<unsigned char>(text[i + 1]) & 0x3F) << 12) |
                   ((static_cast<unsigned char>(text[i + 2]) & 0x3F) << 6) |
                   (static_cast<unsigned char>(text[i + 3]) & 0x3F);
        }
        
        return -1;
    }
    
    size_t getUTF8Length(unsigned char c) {
        if ((c & 0x80) == 0) return 1;
        if ((c & 0xE0) == 0xC0) return 2;
        if ((c & 0xF0) == 0xE0) return 3;
        if ((c & 0xF8) == 0xF0) return 4;
        return 1;
    }
    
    std::string codepointToUTF8(int codepoint) {
        if (codepoint < 0x80) {
            return std::string(1, static_cast<char>(codepoint));
        } else if (codepoint < 0x800) {
            char bytes[2] = {
                static_cast<char>(0xC0 | (codepoint >> 6)),
                static_cast<char>(0x80 | (codepoint & 0x3F))
            };
            return std::string(bytes, 2);
        } else if (codepoint < 0x10000) {
            char bytes[3] = {
                static_cast<char>(0xE0 | (codepoint >> 12)),
                static_cast<char>(0x80 | ((codepoint >> 6) & 0x3F)),
                static_cast<char>(0x80 | (codepoint & 0x3F))
            };
            return std::string(bytes, 3);
        } else {
            char bytes[4] = {
                static_cast<char>(0xF0 | (codepoint >> 18)),
                static_cast<char>(0x80 | ((codepoint >> 12) & 0x3F)),
                static_cast<char>(0x80 | ((codepoint >> 6) & 0x3F)),
                static_cast<char>(0x80 | (codepoint & 0x3F))
            };
            return std::string(bytes, 4);
        }
    }
};

// Static instance
static BPETokenizer* g_tokenizer = nullptr;

std::vector<int> tokenize(const std::string& text, int maxLength) {
    if (!g_tokenizer) {
        g_tokenizer = new BPETokenizer();
    }
    return g_tokenizer->encode(text, maxLength);
}

std::string detokenize(const std::vector<int>& tokens) {
    if (!g_tokenizer) {
        g_tokenizer = new BPETokenizer();
    }
    return g_tokenizer->decode(tokens);
}

int getVocabSize() {
    if (!g_tokenizer) {
        g_tokenizer = new BPETokenizer();
    }
    return g_tokenizer->vocabSize();
}

} // namespace kuaihui
