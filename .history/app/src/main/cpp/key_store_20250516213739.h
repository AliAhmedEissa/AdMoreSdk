#ifndef ADMORE_KEY_STORE_H
#define ADMORE_KEY_STORE_H

#include <jni.h>
#include <string>

class KeyStore {
public:
    static bool storeKey(const std::string& key);
    static std::string getKey();
    static bool deleteKey();
    static bool hasKey();
    
private:
    static const char* KEY_FILE_NAME;
    static const char* KEY_ENCRYPTION_KEY;
    
    static std::string encryptKey(const std::string& key);
    static std::string decryptKey(const std::string& encryptedKey);
    static std::string getKeyFilePath();
};

#endif //ADMORE_KEY_STORE_H 