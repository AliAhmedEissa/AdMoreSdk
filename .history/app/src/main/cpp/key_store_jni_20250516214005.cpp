#include <jni.h>
#include "key_store.h"

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_seamlabs_admore_core_encryption_NativeKeyStore_initialize(
    JNIEnv* env, jobject /* this */, jstring encryptionKey) {
    
    const char* keyStr = env->GetStringUTFChars(encryptionKey, nullptr);
    bool result = KeyStore::initialize(keyStr);
    env->ReleaseStringUTFChars(encryptionKey, keyStr);
    
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_seamlabs_admore_core_encryption_NativeKeyStore_storeKey(
    JNIEnv* env, jobject /* this */, jstring key) {
    
    const char* keyStr = env->GetStringUTFChars(key, nullptr);
    bool result = KeyStore::storeKey(keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_seamlabs_admore_core_encryption_NativeKeyStore_getKey(
    JNIEnv* env, jobject /* this */) {
    
    std::string key = KeyStore::getKey();
    return env->NewStringUTF(key.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_seamlabs_admore_core_encryption_NativeKeyStore_deleteKey(
    JNIEnv* env, jobject /* this */) {
    
    return KeyStore::deleteKey();
}

JNIEXPORT jboolean JNICALL
Java_com_seamlabs_admore_core_encryption_NativeKeyStore_hasKey(
    JNIEnv* env, jobject /* this */) {
    
    return KeyStore::hasKey();
}

} 