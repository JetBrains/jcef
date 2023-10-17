#include "keyboard_utils.h"

#include <jni.h>

#if defined(OS_MAC) || defined(OS_LINUX) || defined(OS_WIN)
// nothing
#elif defined(__APPLE__)
#define OS_MAC
#elif defined(__linux__)
#define OS_LINUX
#elif defined(_WIN32)
#define OS_WIN
#else
std::static_assert(false, "Unknown OS");
#endif

#ifdef OS_MAC
#include <Carbon/Carbon.h>
#endif

namespace jcef_keyboard_utils {

namespace {

class ScopedJNIClass {
 public:
  explicit ScopedJNIClass(JNIEnv* env, jclass cls) : env_(env), jhandle_(cls) {}

  virtual ~ScopedJNIClass() {
    if (jhandle_)
      env_->DeleteLocalRef(jhandle_);
  }

  operator jclass() const { return jhandle_; }

 protected:
  JNIEnv* const env_;
  jclass jhandle_;
};

bool GetJNIFieldStaticInt(JNIEnv* env,
                          jclass cls,
                          const char* field_name,
                          int* value) {
  jfieldID field = env->GetStaticFieldID(cls, field_name, "I");
  if (field) {
    *value = env->GetStaticIntField(cls, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool CallJNIMethodI_V(JNIEnv* env,
                      jclass cls,
                      jobject obj,
                      const char* method_name,
                      int* value) {
  jmethodID methodID = env->GetMethodID(cls, method_name, "()I");
  if (methodID) {
    *value = env->CallIntMethod(obj, methodID);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool CallJNIMethodC_V(JNIEnv* env,
                      jclass cls,
                      jobject obj,
                      const char* method_name,
                      unsigned short* value) {
  jmethodID methodID = env->GetMethodID(cls, method_name, "()C");
  if (methodID) {
    *value = env->CallCharMethod(obj, methodID);
    return true;
  }
  env->ExceptionClear();
  return false;
}

#ifdef OS_WIN
bool GetJNIFieldLong(JNIEnv* env,
                     jclass cls,
                     jobject obj,
                     const char* field_name,
                     jlong* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "J");
  if (field) {
    *value = env->GetLongField(obj, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}
#endif

#define JNI_STATIC(name) _static_##name

#define JNI_STATIC_DEFINE_INT_RV(env, cls, name, rv)                        \
  static int JNI_STATIC(name) = -1;                                         \
  if (JNI_STATIC(name) == -1 && !jcef_keyboard_utils::GetJNIFieldStaticInt( \
                                    env, cls, #name, &JNI_STATIC(name))) {  \
    return rv;                                                              \
  }

int GetCefModifiers(JNIEnv* env, jclass cls, int modifiers) {
  JNI_STATIC_DEFINE_INT_RV(env, cls, ALT_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON1_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON2_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON3_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, CTRL_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, META_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, SHIFT_DOWN_MASK, 0);

  int cef_modifiers = 0;
  if (modifiers & JNI_STATIC(SHIFT_DOWN_MASK))
    cef_modifiers |= (1 << 1);  // cef EVENTFLAG_SHIFT_DOWN;
  if (modifiers & JNI_STATIC(CTRL_DOWN_MASK))
    cef_modifiers |= (1 << 2);  // cef EVENTFLAG_CONTROL_DOWN;
  if (modifiers & JNI_STATIC(ALT_DOWN_MASK))
    cef_modifiers |= (1 << 3);  // cef EVENTFLAG_ALT_DOWN
  if (modifiers & JNI_STATIC(BUTTON1_DOWN_MASK))
    cef_modifiers |= (1 << 4);  // cef EVENTFLAG_LEFT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON2_DOWN_MASK))
    cef_modifiers |= (1 << 5);  // cef EVENTFLAG_MIDDLE_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON3_DOWN_MASK))
    cef_modifiers |= (1 << 6);  // cef EVENTFLAG_RIGHT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(META_DOWN_MASK))
    cef_modifiers |= (1 << 7);  // cef EVENTFLAG_COMMAND_DOWN;

  return cef_modifiers;
}

#ifdef OS_MAC
int GetMacKeyCodeFromChar(int key_char) {
  switch (key_char) {
    case ' ':
      return kVK_Space;
    case '\n':
      return kVK_Return;

    case kEscapeCharCode:
      return kVK_Escape;

    case '0':
    case ')':
      return kVK_ANSI_0;
    case '1':
    case '!':
      return kVK_ANSI_1;
    case '2':
    case '@':
      return kVK_ANSI_2;
    case '3':
    case '#':
      return kVK_ANSI_3;
    case '4':
    case '$':
      return kVK_ANSI_4;
    case '5':
    case '%':
      return kVK_ANSI_5;
    case '6':
    case '^':
      return kVK_ANSI_6;
    case '7':
    case '&':
      return kVK_ANSI_7;
    case '8':
    case '*':
      return kVK_ANSI_8;
    case '9':
    case '(':
      return kVK_ANSI_9;

    case 'a':
    case 'A':
      return kVK_ANSI_A;
    case 'b':
    case 'B':
      return kVK_ANSI_B;
    case 'c':
    case 'C':
      return kVK_ANSI_C;
    case 'd':
    case 'D':
      return kVK_ANSI_D;
    case 'e':
    case 'E':
      return kVK_ANSI_E;
    case 'f':
    case 'F':
      return kVK_ANSI_F;
    case 'g':
    case 'G':
      return kVK_ANSI_G;
    case 'h':
    case 'H':
      return kVK_ANSI_H;
    case 'i':
    case 'I':
      return kVK_ANSI_I;
    case 'j':
    case 'J':
      return kVK_ANSI_J;
    case 'k':
    case 'K':
      return kVK_ANSI_K;
    case 'l':
    case 'L':
      return kVK_ANSI_L;
    case 'm':
    case 'M':
      return kVK_ANSI_M;
    case 'n':
    case 'N':
      return kVK_ANSI_N;
    case 'o':
    case 'O':
      return kVK_ANSI_O;
    case 'p':
    case 'P':
      return kVK_ANSI_P;
    case 'q':
    case 'Q':
      return kVK_ANSI_Q;
    case 'r':
    case 'R':
      return kVK_ANSI_R;
    case 's':
    case 'S':
      return kVK_ANSI_S;
    case 't':
    case 'T':
      return kVK_ANSI_T;
    case 'u':
    case 'U':
      return kVK_ANSI_U;
    case 'v':
    case 'V':
      return kVK_ANSI_V;
    case 'w':
    case 'W':
      return kVK_ANSI_W;
    case 'x':
    case 'X':
      return kVK_ANSI_X;
    case 'y':
    case 'Y':
      return kVK_ANSI_Y;
    case 'z':
    case 'Z':
      return kVK_ANSI_Z;

      // U.S. Specific mappings.  Mileage may vary.
    case ';':
    case ':':
      return kVK_ANSI_Semicolon;
    case '=':
    case '+':
      return kVK_ANSI_Equal;
    case ',':
    case '<':
      return kVK_ANSI_Comma;
    case '-':
    case '_':
      return kVK_ANSI_Minus;
    case '.':
    case '>':
      return kVK_ANSI_Period;
    case '/':
    case '?':
      return kVK_ANSI_Slash;
    case '`':
    case '~':
      return kVK_ANSI_Grave;
    case '[':
    case '{':
      return kVK_ANSI_LeftBracket;
    case '\\':
    case '|':
      return kVK_ANSI_Backslash;
    case ']':
    case '}':
      return kVK_ANSI_RightBracket;
    case '\'':
    case '"':
      return kVK_ANSI_Quote;
  }

  return -1;
}
#endif

}  // namespace

bool javaKeyEventToCef(JNIEnv* env,
                       jobject jKeyEvent,
                       CefKeyEventAttributes* result) {
  ScopedJNIClass cls(env, env->GetObjectClass(jKeyEvent));
  JNI_STATIC_DEFINE_INT_RV(env, cls, KEY_PRESSED, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, KEY_RELEASED, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, KEY_TYPED, false);

  int event_type, modifiers;
  unsigned short key_char;
  if (!CallJNIMethodI_V(env, cls, jKeyEvent, "getID", &event_type) ||
      !CallJNIMethodC_V(env, cls, jKeyEvent, "getKeyChar", &key_char) ||
      !CallJNIMethodI_V(env, cls, jKeyEvent, "getModifiersEx", &modifiers)) {
    return false;
  }

  if (event_type == JNI_STATIC(KEY_PRESSED)) {
    result->type = CefKeyEventType::KEYDOWN;
  } else if (event_type == JNI_STATIC(KEY_RELEASED)) {
    result->type = CefKeyEventType::KEYUP;
  } else if (event_type == JNI_STATIC(KEY_TYPED)) {
    result->type = CefKeyEventType::CHAR;
  } else {
    return false;
  }

  result->modifiers = GetCefModifiers(env, cls, modifiers);

#ifdef OS_WIN
  jlong scanCode, rawCode;
  if (!GetJNIFieldLong(env, cls, jKeyEvent, "rawCode", &rawCode) ||
      !GetJNIFieldLong(env, cls, jKeyEvent, "scancode", &scanCode)) {
    return false;
  }

  result->native_key_code = static_cast<int>(scanCode << 16) |  // key scan code
                            1;  // key repeat count
  if (result->type == CefKeyEventType::KEYDOWN) {
    result->windows_key_code = static_cast<int>(rawCode);
  } else if (result->type == CefKeyEventType::KEYUP) {
    result->windows_key_code = static_cast<int>(rawCode);
    // bits 30 and 31 should always be 1 for WM_KEYUP
    result->native_key_code |= 0xC0000000;
  } else if (result->type == CefKeyEventType::CHAR) {
    result->windows_key_code = key_char == '\n' ? '\r' : key_char;
  } else {
    return false;
  }
#endif

#ifdef OS_MAC
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BACK_SPACE, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DELETE, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CLEAR, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DOWN, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ENTER, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ESCAPE, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_LEFT, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_RIGHT, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_TAB, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_UP, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PAGE_UP, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PAGE_DOWN, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_HOME, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_END, false);

  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F1, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F2, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F3, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F4, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F5, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F6, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F7, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F8, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F9, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F10, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F11, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F12, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F13, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F14, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F15, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F16, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F17, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F18, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F19, false);

  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_META, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SHIFT, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CONTROL, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ALT, false);

  JNI_STATIC_DEFINE_INT_RV(env, cls, KEY_LOCATION_LEFT, false);
  JNI_STATIC_DEFINE_INT_RV(env, cls, KEY_LOCATION_RIGHT, false);

  int key_code, key_location;
  if (!CallJNIMethodI_V(env, cls, jKeyEvent, "getKeyCode", &key_code) ||
      !CallJNIMethodI_V(env, cls, jKeyEvent, "getKeyLocation", &key_location)) {
    return false;
  }

  if (key_code == JNI_STATIC(VK_BACK_SPACE)) {
    result->native_key_code = kVK_Delete;
    result->unmodified_character = kBackspaceCharCode;
  } else if (key_code == JNI_STATIC(VK_DELETE)) {
    result->native_key_code = kVK_ForwardDelete;
    result->unmodified_character = kDeleteCharCode;
  } else if (key_code == JNI_STATIC(VK_CLEAR)) {
    result->native_key_code = kVK_ANSI_KeypadClear;
    result->unmodified_character = /* NSClearLineFunctionKey */ 0xF739;
  } else if (key_code == JNI_STATIC(VK_DOWN)) {
    result->native_key_code = kVK_DownArrow;
    result->unmodified_character = /* NSDownArrowFunctionKey */ 0xF701;
  } else if (key_code == JNI_STATIC(VK_ENTER)) {
    result->native_key_code = kVK_Return;
    result->unmodified_character = kReturnCharCode;
  } else if (key_code == JNI_STATIC(VK_ESCAPE)) {
    result->native_key_code = kVK_Escape;
    result->unmodified_character = kEscapeCharCode;
  } else if (key_code == JNI_STATIC(VK_LEFT)) {
    result->native_key_code = kVK_LeftArrow;
    result->unmodified_character = /* NSLeftArrowFunctionKey */ 0xF702;
  } else if (key_code == JNI_STATIC(VK_RIGHT)) {
    result->native_key_code = kVK_RightArrow;
    result->unmodified_character = /* NSRightArrowFunctionKey */ 0xF703;
  } else if (key_code == JNI_STATIC(VK_TAB)) {
    result->native_key_code = kVK_Tab;
    result->unmodified_character = kTabCharCode;
  } else if (key_code == JNI_STATIC(VK_UP)) {
    result->native_key_code = kVK_UpArrow;
    result->unmodified_character = /* NSUpArrowFunctionKey */ 0xF700;
  } else if (key_code == JNI_STATIC(VK_PAGE_UP)) {
    result->native_key_code = kVK_PageUp;
    result->unmodified_character = kPageUpCharCode;
  } else if (key_code == JNI_STATIC(VK_PAGE_DOWN)) {
    result->native_key_code = kVK_PageDown;
    result->unmodified_character = kPageDownCharCode;
  } else if (key_code == JNI_STATIC(VK_HOME)) {
    result->native_key_code = kVK_Home;
    result->unmodified_character = kHomeCharCode;
  } else if (key_code == JNI_STATIC(VK_END)) {
    result->native_key_code = kVK_End;
    result->unmodified_character = kEndCharCode;
  } else if (key_code == JNI_STATIC(VK_F1)) {
    result->native_key_code = kVK_F1;
    result->unmodified_character = 63236;
  } else if (key_code == JNI_STATIC(VK_F2)) {
    result->native_key_code = kVK_F2;
    result->unmodified_character = 63237;
  } else if (key_code == JNI_STATIC(VK_F3)) {
    result->native_key_code = kVK_F3;
    result->unmodified_character = 63238;
  } else if (key_code == JNI_STATIC(VK_F4)) {
    result->native_key_code = kVK_F4;
    result->unmodified_character = 63239;
  } else if (key_code == JNI_STATIC(VK_F5)) {
    result->native_key_code = kVK_F5;
    result->unmodified_character = 63240;
  } else if (key_code == JNI_STATIC(VK_F6)) {
    result->native_key_code = kVK_F6;
    result->unmodified_character = 63241;
  } else if (key_code == JNI_STATIC(VK_F7)) {
    result->native_key_code = kVK_F7;
    result->unmodified_character = 63242;
  } else if (key_code == JNI_STATIC(VK_F8)) {
    result->native_key_code = kVK_F8;
    result->unmodified_character = 63243;
  } else if (key_code == JNI_STATIC(VK_F9)) {
    result->native_key_code = kVK_F9;
    result->unmodified_character = 63244;
  } else if (key_code == JNI_STATIC(VK_F10)) {
    result->native_key_code = kVK_F10;
    result->unmodified_character = 63245;
  } else if (key_code == JNI_STATIC(VK_F11)) {
    result->native_key_code = kVK_F11;
    result->unmodified_character = 63246;
  } else if (key_code == JNI_STATIC(VK_F12)) {
    result->native_key_code = kVK_F12;
    result->unmodified_character = 63247;
  } else if (key_code == JNI_STATIC(VK_F13)) {
    result->native_key_code = kVK_F13;
    result->unmodified_character = 63248;
  } else if (key_code == JNI_STATIC(VK_F14)) {
    result->native_key_code = kVK_F14;
    result->unmodified_character = 63249;
  } else if (key_code == JNI_STATIC(VK_F15)) {
    result->native_key_code = kVK_F15;
    result->unmodified_character = 63250;
  } else if (key_code == JNI_STATIC(VK_F16)) {
    result->native_key_code = kVK_F16;
    result->unmodified_character = 63251;
  } else if (key_code == JNI_STATIC(VK_F17)) {
    result->native_key_code = kVK_F17;
    result->unmodified_character = 63252;
  } else if (key_code == JNI_STATIC(VK_F18)) {
    result->native_key_code = kVK_F18;
    result->unmodified_character = 63253;
  } else if (key_code == JNI_STATIC(VK_F19)) {
    result->native_key_code = kVK_F19;
    result->unmodified_character = 63254;
  } else if (key_code == JNI_STATIC(VK_META)) {
    result->native_key_code = key_location == JNI_STATIC(KEY_LOCATION_RIGHT)
                                  ? kVK_RightCommand
                                  : kVK_Command;
    result->unmodified_character = 0;
  } else if (key_code == JNI_STATIC(VK_CONTROL)) {
    result->native_key_code = key_location == JNI_STATIC(KEY_LOCATION_RIGHT)
                                  ? kVK_RightControl
                                  : kVK_Control;
    result->unmodified_character = 0;
  } else if (key_code == JNI_STATIC(VK_SHIFT)) {
    result->native_key_code = key_location == JNI_STATIC(KEY_LOCATION_RIGHT)
                                  ? kVK_RightShift
                                  : kVK_Shift;
    result->unmodified_character = 0;
  } else if (key_code == JNI_STATIC(VK_ALT)) {
    result->native_key_code = key_location == JNI_STATIC(KEY_LOCATION_RIGHT)
                                  ? kVK_RightOption
                                  : kVK_Option;
    result->unmodified_character = 0;
  } else {
    result->native_key_code = GetMacKeyCodeFromChar(key_char);
    if (result->native_key_code == -1)
      result->native_key_code = 0;

    if (result->native_key_code == kVK_Return) {
      result->unmodified_character = kReturnCharCode;
    } else {
      result->unmodified_character = key_char;
    }
  }

  result->character = result->unmodified_character;

  // Control characters.
  if (result->modifiers & (1 << 2) /* cef EVENTFLAG_CONTROL_DOWN */) {
    if (key_char >= 'A' && key_char <= 'Z')
      result->character = 1 + key_char - 'A';
    else if (result->native_key_code == kVK_ANSI_LeftBracket)
      result->character = 27;
    else if (result->native_key_code == kVK_ANSI_Backslash)
      result->character = 28;
    else if (result->native_key_code == kVK_ANSI_RightBracket)
      result->character = 29;
  }
#endif

  return true;
}

}  // namespace jcef_keyboard_utils
