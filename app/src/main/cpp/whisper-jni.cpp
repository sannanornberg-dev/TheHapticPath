#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstring>
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_hapticpath_app_WhisperEngine_initContextNative(
        JNIEnv* env,
        jobject thiz,
        jstring model_path_str
) {
    if (!model_path_str) return 0;

    const char* model_path = env->GetStringUTFChars(model_path_str, nullptr);
    LOGI("Laddar Whisper-modell från: %s", model_path);

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    struct whisper_context* ctx = whisper_init_from_file_with_params(model_path, cparams);
    env->ReleaseStringUTFChars(model_path_str, model_path);

    if (!ctx) {
        LOGE("Misslyckades att initiera whisper_context");
        return 0;
    }

    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_hapticpath_app_WhisperEngine_freeContextNative(
        JNIEnv* env,
        jobject thiz,
        jlong context_ptr
) {
    struct whisper_context* ctx = reinterpret_cast<struct whisper_context*>(context_ptr);
    if (ctx) {
        whisper_free(ctx);
        LOGI("Whisper-kontext frigjord");
    }
}

JNIEXPORT jstring JNICALL
Java_com_hapticpath_app_WhisperEngine_transcribeBufferNative(
        JNIEnv* env,
        jobject thiz,
        jlong context_ptr,
        jfloatArray samples,
        jint num_samples
) {
    struct whisper_context* ctx = reinterpret_cast<struct whisper_context*>(context_ptr);
    if (!ctx || !samples || num_samples <= 0) return env->NewStringUTF("");

    LOGI("Startar transkribering av %d samples...", num_samples);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = 4;            // Nyttjar alla 4 prestandakärnor på Helio G85
    params.max_tokens = 32;           // Begränsar responslängd för realtidsbehov
    params.single_segment = true;
    params.no_timestamps = true;
    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.language = "sv";          // Lås till svenska

    // Stabiliseringsparametrar mot spökord & felaktiga meningsuppstarter
    params.temperature = 0.0f;       // Eliminera slumpmässighet
    params.initial_prompt = "Svenska meningar för SFI och ordföljd. Jag gick inte. Igår kom jag.";

    jfloat* samples_ptr = env->GetFloatArrayElements(samples, nullptr);
    if (!samples_ptr) {
        LOGE("Kunde inte hämta samplingspekare från JNI");
        return env->NewStringUTF("");
    }

    int state = whisper_full(ctx, params, samples_ptr, num_samples);
    env->ReleaseFloatArrayElements(samples, samples_ptr, JNI_ABORT);

    if (state != 0) {
        LOGE("Fel vid körning av whisper_full, kod: %d", state);
        return env->NewStringUTF("");
    }

    std::string result = "";
    int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        if (text) {
            result += text;
        }
    }

    LOGI("Resultat från C++: %s", result.c_str());
    return env->NewStringUTF(result.c_str());
}

#ifdef __cplusplus
}
#endif