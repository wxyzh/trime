#ifndef TRIME_JNI_UTILS_H
#define TRIME_JNI_UTILS_H

#include <jni.h>
#include <string>

static inline void throwJavaException(JNIEnv *env, const char *msg) {
    jclass c = env->FindClass("java/lang/Exception");
    env->ThrowNew(c, msg);
    env->DeleteLocalRef(c);
}

class CString {
private:
    JNIEnv *env_;
    jstring str_;
    const char *chr_;

public:
    CString(JNIEnv *env, jstring str)
            : env_(env), str_(str), chr_(env->GetStringUTFChars(str, nullptr)) {}

    ~CString() {
        env_->ReleaseStringUTFChars(str_, chr_);
    }

    operator std::string() { return chr_; }

    operator const char *() { return chr_; }

    const char *operator*() { return chr_; }
};

template<typename T = jobject>
class JRef {
private:
    JNIEnv *env_;
    T ref_;

public:
    JRef(JNIEnv *env, jobject ref) : env_(env), ref_(reinterpret_cast<T>(ref)) {}

    ~JRef() {
        env_->DeleteLocalRef(ref_);
    }

    operator T() { return ref_; }

    T operator*() { return ref_; }
};

class JString {
private:
    JNIEnv *env_;
    jstring jstring_;

public:
    JString(JNIEnv *env, const char *chars)
            : env_(env), jstring_(env->NewStringUTF(chars)) {}

    JString(JNIEnv *env, const std::string &string)
            : JString(env, string.c_str()) {}

    ~JString() {
        env_->DeleteLocalRef(jstring_);
    }

    operator jstring() { return jstring_; }

    jstring operator*() { return jstring_; }
};

class JClass {
private:
    JNIEnv *env_;
    jclass jclass_;

public:
    JClass(JNIEnv *env, const char *name)
            : env_(env), jclass_(env->FindClass(name)) {}

    ~JClass() {
        env_->DeleteLocalRef(jclass_);
    }

    operator jclass() { return jclass_; }

    jclass operator*() { return jclass_; }
};

class JEnv {
private:
    JNIEnv *env;

public:
    JEnv(JavaVM *jvm) {
        if (jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
            jvm->AttachCurrentThread(&env, nullptr);
        }
    }

    operator JNIEnv *() { return env; }

    JNIEnv *operator->() { return env; }
};

class GlobalRefSingleton {
public:
    JavaVM *jvm;

    jclass Object;

    jclass String;

    jclass Integer;
    jmethodID IntegerInit;

    jclass Boolean;
    jmethodID BooleanInit;

    jclass HashMap;
    jmethodID HashMapInit;
    jmethodID HashMapPut;

    jclass ArrayList;
    jmethodID ArrayListInit;
    jmethodID ArrayListAdd;

    jclass Pair;
    jmethodID PairFirst;
    jmethodID PairSecond;

    jclass Rime;
    jmethodID HandleRimeNotification;

    jclass RimeComposition;
    jfieldID RimeCompositionLength;
    jfieldID RimeCompositionCursorPos;
    jfieldID RimeCompositionSelStart;
    jfieldID RimeCompositionSelEnd;
    jfieldID RimeCompositionPreedit;

    jclass CandidateListItem;
    jmethodID CandidateListItemInit;

    jclass RimeCommit;
    jfieldID RimeCommitText;

    jclass RimeContext;
    jfieldID RimeContextComposition;
    jfieldID RimeContextMenu;
    jfieldID RimeContextCommitTextPreview;
    jfieldID RimeContextSelectLabels;

    jclass RimeMenu;
    jfieldID RimeMenuPageSize;
    jfieldID RimeMenuPageNo;
    jfieldID RimeMenuIsLastPage;
    jfieldID RimeMenuHighlightedCandidateIndex;
    jfieldID RimeMenuNumCandidates;
    jfieldID RimeMenuCandidates;

    jclass RimeStatus;
    jfieldID RimeStatusSchemaId;
    jfieldID RimeStatusSchemaName;
    jfieldID RimeStatusDisable;
    jfieldID RimeStatusComposing;
    jfieldID RimeStatusAsciiMode;
    jfieldID RimeStatusFullShape;
    jfieldID RimeStatusSimplified;
    jfieldID RimeStatusTraditional;
    jfieldID RimeStatusAsciiPunct;

    jclass SchemaListItem;
    jmethodID SchemaListItemInit;

    GlobalRefSingleton(JavaVM *jvm_) : jvm(jvm_) {
        JNIEnv *env;
        jvm->AttachCurrentThread(&env, nullptr);

        Object = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Object")));

        String = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/String")));

        Integer = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Integer")));
        IntegerInit = env->GetMethodID(Integer, "<init>", "(I)V");

        Boolean = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Boolean")));
        BooleanInit = env->GetMethodID(Boolean, "<init>", "(Z)V");

        HashMap = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/HashMap")));
        HashMapInit = env->GetMethodID(HashMap, "<init>", "()V");
        HashMapPut = env->GetMethodID(HashMap, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        ArrayList = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
        ArrayListInit = env->GetMethodID(ArrayList, "<init>", "(I)V");
        ArrayListAdd = env->GetMethodID(ArrayList, "add", "(ILjava/lang/Object;)V");

        Pair = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("kotlin/Pair")));
        PairFirst = env->GetMethodID(Pair, "getFirst", "()Ljava/lang/Object;");
        PairSecond = env->GetMethodID(Pair, "getSecond", "()Ljava/lang/Object;");

        Rime = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/Rime")));
        HandleRimeNotification = env->GetStaticMethodID(Rime, "handleRimeNotification", "(Ljava/lang/String;Ljava/lang/String;)V");

        RimeComposition = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/Rime$RimeComposition")));
        RimeCompositionLength = env->GetFieldID(RimeComposition, "length", "I");
        RimeCompositionCursorPos = env->GetFieldID(RimeComposition, "cursor_pos", "I");
        RimeCompositionSelStart = env->GetFieldID(RimeComposition, "sel_start", "I");
        RimeCompositionSelEnd = env->GetFieldID(RimeComposition, "sel_end", "I");
        RimeCompositionPreedit = env->GetFieldID(RimeComposition, "preedit", "Ljava/lang/String;");

        CandidateListItem = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/CandidateListItem")));
        CandidateListItemInit = env->GetMethodID(CandidateListItem, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");

        RimeCommit = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/Rime$RimeCommit")));
        RimeCommitText = env->GetFieldID(RimeCommit, "text", "Ljava/lang/String;");

        RimeContext = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/Rime$RimeContext")));
        RimeContextComposition = env->GetFieldID(RimeContext, "composition", "Lcom/osfans/trime/core/Rime$RimeComposition;");
        RimeContextMenu = env->GetFieldID(RimeContext, "menu", "Lcom/osfans/trime/core/Rime$RimeMenu;");
        RimeContextCommitTextPreview = env->GetFieldID(RimeContext, "commit_text_preview", "Ljava/lang/String;");
        RimeContextSelectLabels = env->GetFieldID(RimeContext, "select_labels", "[Ljava/lang/String;");

        RimeMenu = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/Rime$RimeMenu")));
        RimeMenuPageSize = env->GetFieldID(RimeMenu, "page_size", "I");
        RimeMenuPageNo = env->GetFieldID(RimeMenu, "page_no", "I");
        RimeMenuIsLastPage = env->GetFieldID(RimeMenu, "is_last_page", "Z");
        RimeMenuHighlightedCandidateIndex = env->GetFieldID(RimeMenu, "highlighted_candidate_index", "I");
        RimeMenuNumCandidates = env->GetFieldID(RimeMenu, "num_candidates", "I");
        RimeMenuCandidates = env->GetFieldID(RimeMenu, "candidates", "[Lcom/osfans/trime/core/CandidateListItem;");

        RimeStatus = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/Rime$RimeStatus")));
        RimeStatusSchemaId = env->GetFieldID(RimeStatus, "schema_id", "Ljava/lang/String;");
        RimeStatusSchemaName = env->GetFieldID(RimeStatus, "schema_name", "Ljava/lang/String;");
        RimeStatusDisable = env->GetFieldID(RimeStatus, "is_disabled", "Z");
        RimeStatusComposing = env->GetFieldID(RimeStatus, "is_composing", "Z");
        RimeStatusAsciiMode = env->GetFieldID(RimeStatus, "is_ascii_mode", "Z");
        RimeStatusFullShape = env->GetFieldID(RimeStatus, "is_full_shape", "Z");
        RimeStatusSimplified = env->GetFieldID(RimeStatus, "is_simplified", "Z");
        RimeStatusTraditional = env->GetFieldID(RimeStatus, "is_traditional", "Z");
        RimeStatusAsciiPunct = env->GetFieldID(RimeStatus, "is_ascii_punct", "Z");

        SchemaListItem = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("com/osfans/trime/core/SchemaListItem")));
        SchemaListItemInit = env->GetMethodID(SchemaListItem, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
    }

    const JEnv AttachEnv() const { return JEnv(jvm); }
};

extern GlobalRefSingleton *GlobalRef;

#endif //TRIME_JNI_UTILS_H
