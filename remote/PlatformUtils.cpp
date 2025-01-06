#include "PlatformUtils.h"

#include "../native/keyboard_utils.h"

jobject Java_com_jetbrains_cef_remote_PlatformUtils_toCefKeyEvent(
    JNIEnv* env,
    jclass,
    jobject jKeyEvent) {
  jcef_keyboard_utils::CefKeyEventAttributes attributes{};
  if (!jcef_keyboard_utils::javaKeyEventToCef(env, jKeyEvent, &attributes)) {
    return nullptr;
  }

  jclass jCefKeyEventClass =
      env->FindClass("com/jetbrains/cef/remote/thrift_codegen/CefKeyEvent");
  if (!jCefKeyEventClass) {
    return nullptr;
  }

  jmethodID jConstructorID =
      env->GetMethodID(jCefKeyEventClass, "<init>", "(IIIISSZ)V");
  if (!jConstructorID) {
    env->DeleteLocalRef(jCefKeyEventClass);
    return nullptr;
  }

  jobject jCefKeyEvent =
      env->NewObject(jCefKeyEventClass, jConstructorID, attributes.type,
                     attributes.modifiers, attributes.windows_key_code,
                     attributes.native_key_code, attributes.character,
                     attributes.unmodified_character, attributes.is_system_key);

  env->DeleteLocalRef(jCefKeyEventClass);
  return jCefKeyEvent;
}
