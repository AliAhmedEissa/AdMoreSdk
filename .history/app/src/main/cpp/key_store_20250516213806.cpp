#include "key_store.h"
#include <fstream>
#include <sstream>
#include <openssl/aes.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <android/log.h>
#include <sys/stat.h>
#include <unistd.h>

#define LOG_TAG "AdMoreKeyStore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const char* KeyStore::KEY_FILE_NAME = "admore_key.dat";
const char* KeyStore::KEY_ENCRYPTION_KEY = "your_hardcoded_encryption_key_here";

std::string KeyStore::getKeyFilePath() {
    // Get the app's private directory
    const char* privateDir = "/data/data/com.seamlabs.admore/files";
    std::string filePath = std::string(privateDir) + "/" + KEY_FILE_NAME;
    return filePath;
}

bool KeyStore::storeKey(const std::string& key) {
    try {
        std::string encryptedKey = encryptKey(key);
        std::string filePath = getKeyFilePath();
        
        // Create directory if it doesn't exist
        mkdir("/data/data/com.seamlabs.admore/files", 0700);
        
        // Set file permissions to be accessible only by the app
        std::ofstream file(filePath, std::ios::binary);
        if (!file.is_open()) {
            LOGE("Failed to open file for writing");
            return false;
        }
        
        file << encryptedKey;
        file.close();
        
        // Set file permissions
        chmod(filePath.c_str(), 0600);
        
        return true;
    } catch (const std::exception& e) {
        LOGE("Error storing key: %s", e.what());
        return false;
    }
}

std::string KeyStore::getKey() {
    try {
        std::string filePath = getKeyFilePath();
        std::ifstream file(filePath, std::ios::binary);
        
        if (!file.is_open()) {
            LOGE("Failed to open file for reading");
            return "";
        }
        
        std::stringstream buffer;
        buffer << file.rdbuf();
        std::string encryptedKey = buffer.str();
        file.close();
        
        return decryptKey(encryptedKey);
    } catch (const std::exception& e) {
        LOGE("Error reading key: %s", e.what());
        return "";
    }
}

bool KeyStore::deleteKey() {
    try {
        std::string filePath = getKeyFilePath();
        return remove(filePath.c_str()) == 0;
    } catch (const std::exception& e) {
        LOGE("Error deleting key: %s", e.what());
        return false;
    }
}

bool KeyStore::hasKey() {
    try {
        std::string filePath = getKeyFilePath();
        std::ifstream file(filePath);
        return file.good();
    } catch (const std::exception& e) {
        LOGE("Error checking key: %s", e.what());
        return false;
    }
}

std::string KeyStore::encryptKey(const std::string& key) {
    try {
        // Initialize encryption context
        EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
        if (!ctx) {
            throw std::runtime_error("Failed to create cipher context");
        }

        // Generate random IV
        unsigned char iv[16];
        if (RAND_bytes(iv, sizeof(iv)) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to generate IV");
        }

        // Initialize encryption
        if (EVP_EncryptInit_ex(ctx, EVP_aes_256_cbc(), nullptr, 
            reinterpret_cast<const unsigned char*>(KEY_ENCRYPTION_KEY), iv) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to initialize encryption");
        }

        // Prepare output buffer
        int len = key.length() + EVP_MAX_BLOCK_LENGTH;
        unsigned char* outbuf = new unsigned char[len];
        int outlen = 0;

        // Encrypt the data
        if (EVP_EncryptUpdate(ctx, outbuf, &outlen,
            reinterpret_cast<const unsigned char*>(key.c_str()), key.length()) != 1) {
            delete[] outbuf;
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to encrypt data");
        }

        int tmplen = 0;
        if (EVP_EncryptFinal_ex(ctx, outbuf + outlen, &tmplen) != 1) {
            delete[] outbuf;
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to finalize encryption");
        }

        outlen += tmplen;
        EVP_CIPHER_CTX_free(ctx);

        // Combine IV and encrypted data
        std::string result;
        result.append(reinterpret_cast<char*>(iv), sizeof(iv));
        result.append(reinterpret_cast<char*>(outbuf), outlen);
        
        delete[] outbuf;
        return result;
    } catch (const std::exception& e) {
        LOGE("Encryption error: %s", e.what());
        throw;
    }
}

std::string KeyStore::decryptKey(const std::string& encryptedKey) {
    try {
        if (encryptedKey.length() < 16) {
            throw std::runtime_error("Invalid encrypted data");
        }

        // Extract IV
        unsigned char iv[16];
        std::copy(encryptedKey.begin(), encryptedKey.begin() + 16, iv);

        // Initialize decryption context
        EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
        if (!ctx) {
            throw std::runtime_error("Failed to create cipher context");
        }

        // Initialize decryption
        if (EVP_DecryptInit_ex(ctx, EVP_aes_256_cbc(), nullptr,
            reinterpret_cast<const unsigned char*>(KEY_ENCRYPTION_KEY), iv) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to initialize decryption");
        }

        // Prepare output buffer
        int len = encryptedKey.length() - 16;
        unsigned char* outbuf = new unsigned char[len];
        int outlen = 0;

        // Decrypt the data
        if (EVP_DecryptUpdate(ctx, outbuf, &outlen,
            reinterpret_cast<const unsigned char*>(encryptedKey.c_str() + 16), len) != 1) {
            delete[] outbuf;
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to decrypt data");
        }

        int tmplen = 0;
        if (EVP_DecryptFinal_ex(ctx, outbuf + outlen, &tmplen) != 1) {
            delete[] outbuf;
            EVP_CIPHER_CTX_free(ctx);
            throw std::runtime_error("Failed to finalize decryption");
        }

        outlen += tmplen;
        EVP_CIPHER_CTX_free(ctx);

        std::string result(reinterpret_cast<char*>(outbuf), outlen);
        delete[] outbuf;
        return result;
    } catch (const std::exception& e) {
        LOGE("Decryption error: %s", e.what());
        throw;
    }
} 