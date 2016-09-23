#include <jni.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_nomenas_jniexample_MainActivity_intFromJNI(JNIEnv *env, jobject instance) {

    return 11;

}

JNIEXPORT jstring JNICALL
Java_com_nomenas_jniexample_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

#ifdef __cplusplus
}
#endif
