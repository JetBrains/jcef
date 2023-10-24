//
// Created by Vladimir.Kharitonov on 10/12/23.
//

#ifndef JCEF_KEYBOARD_UTILS_H
#define JCEF_KEYBOARD_UTILS_H

#include <stdint.h>

#include <jni.h>

/**
 * The structure contains CefKeyEvent members
 */

namespace jcef_keyboard_utils {

struct CefKeyEventAttributes {
  int type;
  uint32_t modifiers;
  int windows_key_code;
  int native_key_code;
  int is_system_key;
  char16_t character;
  char16_t unmodified_character;
};

bool javaKeyEventToCef(JNIEnv* env,
                       jobject jKeyEvent,
                       CefKeyEventAttributes* result);

}  // namespace jcef_keyboard_utils

#endif  // JCEF_KEYBOARD_UTILS_H
