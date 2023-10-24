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

#if defined(OS_LINUX)
#define XK_3270  // for XK_3270_BackTab
#include <X11/XF86keysym.h>
#include <X11/keysym.h>

int JavaKeyCode2X11(JNIEnv* env, jclass cls/*KeyEvent*/, int keycode);

#endif

#if defined(OS_MAC)
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

// from cef_types.h
enum CefModifiers {
  EVENTFLAG_NONE = 0,
  EVENTFLAG_CAPS_LOCK_ON = 1 << 0,
  EVENTFLAG_SHIFT_DOWN = 1 << 1,
  EVENTFLAG_CONTROL_DOWN = 1 << 2,
  EVENTFLAG_ALT_DOWN = 1 << 3,
  EVENTFLAG_LEFT_MOUSE_BUTTON = 1 << 4,
  EVENTFLAG_MIDDLE_MOUSE_BUTTON = 1 << 5,
  EVENTFLAG_RIGHT_MOUSE_BUTTON = 1 << 6,
  /// Mac OS-X command key.
  EVENTFLAG_COMMAND_DOWN = 1 << 7,
  EVENTFLAG_NUM_LOCK_ON = 1 << 8,
  EVENTFLAG_IS_KEY_PAD = 1 << 9,
  EVENTFLAG_IS_LEFT = 1 << 10,
  EVENTFLAG_IS_RIGHT = 1 << 11,
  EVENTFLAG_ALTGR_DOWN = 1 << 12,
  EVENTFLAG_IS_REPEAT = 1 << 13,
};

enum CefKeyEventType {
  KEYDOWN = 0,
  KEYUP = 2,
  CHAR = 3
};


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
    cef_modifiers |= CefModifiers::EVENTFLAG_SHIFT_DOWN;
  if (modifiers & JNI_STATIC(CTRL_DOWN_MASK))
    cef_modifiers |= CefModifiers::EVENTFLAG_CONTROL_DOWN;
  if (modifiers & JNI_STATIC(ALT_DOWN_MASK))
    cef_modifiers |= CefModifiers::EVENTFLAG_ALT_DOWN;
  if (modifiers & JNI_STATIC(BUTTON1_DOWN_MASK))
    cef_modifiers |= CefModifiers::EVENTFLAG_LEFT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON2_DOWN_MASK))
    cef_modifiers |= CefModifiers::EVENTFLAG_MIDDLE_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON3_DOWN_MASK))
    cef_modifiers |= CefModifiers::EVENTFLAG_RIGHT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(META_DOWN_MASK))
    cef_modifiers |= CefModifiers::EVENTFLAG_COMMAND_DOWN;

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

#if defined(OS_LINUX)
// From ui/events/keycodes/keyboard_codes_posix.h.
enum KeyboardCode {
  VKEY_BACK = 0x08,
  VKEY_TAB = 0x09,
  VKEY_BACKTAB = 0x0A,
  VKEY_CLEAR = 0x0C,
  VKEY_RETURN = 0x0D,
  VKEY_SHIFT = 0x10,
  VKEY_CONTROL = 0x11,
  VKEY_MENU = 0x12,
  VKEY_PAUSE = 0x13,
  VKEY_CAPITAL = 0x14,
  VKEY_KANA = 0x15,
  VKEY_HANGUL = 0x15,
  VKEY_JUNJA = 0x17,
  VKEY_FINAL = 0x18,
  VKEY_HANJA = 0x19,
  VKEY_KANJI = 0x19,
  VKEY_ESCAPE = 0x1B,
  VKEY_CONVERT = 0x1C,
  VKEY_NONCONVERT = 0x1D,
  VKEY_ACCEPT = 0x1E,
  VKEY_MODECHANGE = 0x1F,
  VKEY_SPACE = 0x20,
  VKEY_PRIOR = 0x21,
  VKEY_NEXT = 0x22,
  VKEY_END = 0x23,
  VKEY_HOME = 0x24,
  VKEY_LEFT = 0x25,
  VKEY_UP = 0x26,
  VKEY_RIGHT = 0x27,
  VKEY_DOWN = 0x28,
  VKEY_SELECT = 0x29,
  VKEY_PRINT = 0x2A,
  VKEY_EXECUTE = 0x2B,
  VKEY_SNAPSHOT = 0x2C,
  VKEY_INSERT = 0x2D,
  VKEY_DELETE = 0x2E,
  VKEY_HELP = 0x2F,
  VKEY_0 = 0x30,
  VKEY_1 = 0x31,
  VKEY_2 = 0x32,
  VKEY_3 = 0x33,
  VKEY_4 = 0x34,
  VKEY_5 = 0x35,
  VKEY_6 = 0x36,
  VKEY_7 = 0x37,
  VKEY_8 = 0x38,
  VKEY_9 = 0x39,
  VKEY_A = 0x41,
  VKEY_B = 0x42,
  VKEY_C = 0x43,
  VKEY_D = 0x44,
  VKEY_E = 0x45,
  VKEY_F = 0x46,
  VKEY_G = 0x47,
  VKEY_H = 0x48,
  VKEY_I = 0x49,
  VKEY_J = 0x4A,
  VKEY_K = 0x4B,
  VKEY_L = 0x4C,
  VKEY_M = 0x4D,
  VKEY_N = 0x4E,
  VKEY_O = 0x4F,
  VKEY_P = 0x50,
  VKEY_Q = 0x51,
  VKEY_R = 0x52,
  VKEY_S = 0x53,
  VKEY_T = 0x54,
  VKEY_U = 0x55,
  VKEY_V = 0x56,
  VKEY_W = 0x57,
  VKEY_X = 0x58,
  VKEY_Y = 0x59,
  VKEY_Z = 0x5A,
  VKEY_LWIN = 0x5B,
  VKEY_COMMAND = VKEY_LWIN,  // Provide the Mac name for convenience.
  VKEY_RWIN = 0x5C,
  VKEY_APPS = 0x5D,
  VKEY_SLEEP = 0x5F,
  VKEY_NUMPAD0 = 0x60,
  VKEY_NUMPAD1 = 0x61,
  VKEY_NUMPAD2 = 0x62,
  VKEY_NUMPAD3 = 0x63,
  VKEY_NUMPAD4 = 0x64,
  VKEY_NUMPAD5 = 0x65,
  VKEY_NUMPAD6 = 0x66,
  VKEY_NUMPAD7 = 0x67,
  VKEY_NUMPAD8 = 0x68,
  VKEY_NUMPAD9 = 0x69,
  VKEY_MULTIPLY = 0x6A,
  VKEY_ADD = 0x6B,
  VKEY_SEPARATOR = 0x6C,
  VKEY_SUBTRACT = 0x6D,
  VKEY_DECIMAL = 0x6E,
  VKEY_DIVIDE = 0x6F,
  VKEY_F1 = 0x70,
  VKEY_F2 = 0x71,
  VKEY_F3 = 0x72,
  VKEY_F4 = 0x73,
  VKEY_F5 = 0x74,
  VKEY_F6 = 0x75,
  VKEY_F7 = 0x76,
  VKEY_F8 = 0x77,
  VKEY_F9 = 0x78,
  VKEY_F10 = 0x79,
  VKEY_F11 = 0x7A,
  VKEY_F12 = 0x7B,
  VKEY_F13 = 0x7C,
  VKEY_F14 = 0x7D,
  VKEY_F15 = 0x7E,
  VKEY_F16 = 0x7F,
  VKEY_F17 = 0x80,
  VKEY_F18 = 0x81,
  VKEY_F19 = 0x82,
  VKEY_F20 = 0x83,
  VKEY_F21 = 0x84,
  VKEY_F22 = 0x85,
  VKEY_F23 = 0x86,
  VKEY_F24 = 0x87,
  VKEY_NUMLOCK = 0x90,
  VKEY_SCROLL = 0x91,
  VKEY_LSHIFT = 0xA0,
  VKEY_RSHIFT = 0xA1,
  VKEY_LCONTROL = 0xA2,
  VKEY_RCONTROL = 0xA3,
  VKEY_LMENU = 0xA4,
  VKEY_RMENU = 0xA5,
  VKEY_BROWSER_BACK = 0xA6,
  VKEY_BROWSER_FORWARD = 0xA7,
  VKEY_BROWSER_REFRESH = 0xA8,
  VKEY_BROWSER_STOP = 0xA9,
  VKEY_BROWSER_SEARCH = 0xAA,
  VKEY_BROWSER_FAVORITES = 0xAB,
  VKEY_BROWSER_HOME = 0xAC,
  VKEY_VOLUME_MUTE = 0xAD,
  VKEY_VOLUME_DOWN = 0xAE,
  VKEY_VOLUME_UP = 0xAF,
  VKEY_MEDIA_NEXT_TRACK = 0xB0,
  VKEY_MEDIA_PREV_TRACK = 0xB1,
  VKEY_MEDIA_STOP = 0xB2,
  VKEY_MEDIA_PLAY_PAUSE = 0xB3,
  VKEY_MEDIA_LAUNCH_MAIL = 0xB4,
  VKEY_MEDIA_LAUNCH_MEDIA_SELECT = 0xB5,
  VKEY_MEDIA_LAUNCH_APP1 = 0xB6,
  VKEY_MEDIA_LAUNCH_APP2 = 0xB7,
  VKEY_OEM_1 = 0xBA,
  VKEY_OEM_PLUS = 0xBB,
  VKEY_OEM_COMMA = 0xBC,
  VKEY_OEM_MINUS = 0xBD,
  VKEY_OEM_PERIOD = 0xBE,
  VKEY_OEM_2 = 0xBF,
  VKEY_OEM_3 = 0xC0,
  VKEY_OEM_4 = 0xDB,
  VKEY_OEM_5 = 0xDC,
  VKEY_OEM_6 = 0xDD,
  VKEY_OEM_7 = 0xDE,
  VKEY_OEM_8 = 0xDF,
  VKEY_OEM_102 = 0xE2,
  VKEY_OEM_103 = 0xE3,  // GTV KEYCODE_MEDIA_REWIND
  VKEY_OEM_104 = 0xE4,  // GTV KEYCODE_MEDIA_FAST_FORWARD
  VKEY_PROCESSKEY = 0xE5,
  VKEY_PACKET = 0xE7,
  VKEY_DBE_SBCSCHAR = 0xF3,
  VKEY_DBE_DBCSCHAR = 0xF4,
  VKEY_ATTN = 0xF6,
  VKEY_CRSEL = 0xF7,
  VKEY_EXSEL = 0xF8,
  VKEY_EREOF = 0xF9,
  VKEY_PLAY = 0xFA,
  VKEY_ZOOM = 0xFB,
  VKEY_NONAME = 0xFC,
  VKEY_PA1 = 0xFD,
  VKEY_OEM_CLEAR = 0xFE,
  VKEY_UNKNOWN = 0,

  // POSIX specific VKEYs. Note that as of Windows SDK 7.1, 0x97-9F, 0xD8-DA,
  // and 0xE8 are unassigned.
  VKEY_WLAN = 0x97,
  VKEY_POWER = 0x98,
  VKEY_BRIGHTNESS_DOWN = 0xD8,
  VKEY_BRIGHTNESS_UP = 0xD9,
  VKEY_KBD_BRIGHTNESS_DOWN = 0xDA,
  VKEY_KBD_BRIGHTNESS_UP = 0xE8,

  // Windows does not have a specific key code for AltGr. We use the unused 0xE1
  // (VK_OEM_AX) code to represent AltGr, matching the behaviour of Firefox on
  // Linux.
  VKEY_ALTGR = 0xE1,
  // Windows does not have a specific key code for Compose. We use the unused
  // 0xE6 (VK_ICO_CLEAR) code to represent Compose.
  VKEY_COMPOSE = 0xE6,
};

// From ui/events/keycodes/keyboard_code_conversion_x.cc.
KeyboardCode KeyboardCodeFromXKeysym(unsigned int keysym) {
  switch (keysym) {
    case XK_BackSpace:
      return VKEY_BACK;
    case XK_Delete:
    case XK_KP_Delete:
      return VKEY_DELETE;
    case XK_Tab:
    case XK_KP_Tab:
    case XK_ISO_Left_Tab:
    case XK_3270_BackTab:
      return VKEY_TAB;
    case XK_Linefeed:
    case XK_Return:
    case XK_KP_Enter:
    case XK_ISO_Enter:
      return VKEY_RETURN;
    case XK_Clear:
    case XK_KP_Begin:  // NumPad 5 without Num Lock, for crosbug.com/29169.
      return VKEY_CLEAR;
    case XK_KP_Space:
    case XK_space:
      return VKEY_SPACE;
    case XK_Home:
    case XK_KP_Home:
      return VKEY_HOME;
    case XK_End:
    case XK_KP_End:
      return VKEY_END;
    case XK_Page_Up:
    case XK_KP_Page_Up:  // aka XK_KP_Prior
      return VKEY_PRIOR;
    case XK_Page_Down:
    case XK_KP_Page_Down:  // aka XK_KP_Next
      return VKEY_NEXT;
    case XK_Left:
    case XK_KP_Left:
      return VKEY_LEFT;
    case XK_Right:
    case XK_KP_Right:
      return VKEY_RIGHT;
    case XK_Down:
    case XK_KP_Down:
      return VKEY_DOWN;
    case XK_Up:
    case XK_KP_Up:
      return VKEY_UP;
    case XK_Escape:
      return VKEY_ESCAPE;
    case XK_Kana_Lock:
    case XK_Kana_Shift:
      return VKEY_KANA;
    case XK_Hangul:
      return VKEY_HANGUL;
    case XK_Hangul_Hanja:
      return VKEY_HANJA;
    case XK_Kanji:
      return VKEY_KANJI;
    case XK_Henkan:
      return VKEY_CONVERT;
    case XK_Muhenkan:
      return VKEY_NONCONVERT;
    case XK_Zenkaku_Hankaku:
      return VKEY_DBE_DBCSCHAR;
    case XK_A:
    case XK_a:
      return VKEY_A;
    case XK_B:
    case XK_b:
      return VKEY_B;
    case XK_C:
    case XK_c:
      return VKEY_C;
    case XK_D:
    case XK_d:
      return VKEY_D;
    case XK_E:
    case XK_e:
      return VKEY_E;
    case XK_F:
    case XK_f:
      return VKEY_F;
    case XK_G:
    case XK_g:
      return VKEY_G;
    case XK_H:
    case XK_h:
      return VKEY_H;
    case XK_I:
    case XK_i:
      return VKEY_I;
    case XK_J:
    case XK_j:
      return VKEY_J;
    case XK_K:
    case XK_k:
      return VKEY_K;
    case XK_L:
    case XK_l:
      return VKEY_L;
    case XK_M:
    case XK_m:
      return VKEY_M;
    case XK_N:
    case XK_n:
      return VKEY_N;
    case XK_O:
    case XK_o:
      return VKEY_O;
    case XK_P:
    case XK_p:
      return VKEY_P;
    case XK_Q:
    case XK_q:
      return VKEY_Q;
    case XK_R:
    case XK_r:
      return VKEY_R;
    case XK_S:
    case XK_s:
      return VKEY_S;
    case XK_T:
    case XK_t:
      return VKEY_T;
    case XK_U:
    case XK_u:
      return VKEY_U;
    case XK_V:
    case XK_v:
      return VKEY_V;
    case XK_W:
    case XK_w:
      return VKEY_W;
    case XK_X:
    case XK_x:
      return VKEY_X;
    case XK_Y:
    case XK_y:
      return VKEY_Y;
    case XK_Z:
    case XK_z:
      return VKEY_Z;

    case XK_0:
    case XK_1:
    case XK_2:
    case XK_3:
    case XK_4:
    case XK_5:
    case XK_6:
    case XK_7:
    case XK_8:
    case XK_9:
      return static_cast<KeyboardCode>(VKEY_0 + (keysym - XK_0));

    case XK_parenright:
      return VKEY_0;
    case XK_exclam:
      return VKEY_1;
    case XK_at:
      return VKEY_2;
    case XK_numbersign:
      return VKEY_3;
    case XK_dollar:
      return VKEY_4;
    case XK_percent:
      return VKEY_5;
    case XK_asciicircum:
      return VKEY_6;
    case XK_ampersand:
      return VKEY_7;
    case XK_asterisk:
      return VKEY_8;
    case XK_parenleft:
      return VKEY_9;

    case XK_KP_0:
    case XK_KP_1:
    case XK_KP_2:
    case XK_KP_3:
    case XK_KP_4:
    case XK_KP_5:
    case XK_KP_6:
    case XK_KP_7:
    case XK_KP_8:
    case XK_KP_9:
      return static_cast<KeyboardCode>(VKEY_NUMPAD0 + (keysym - XK_KP_0));

    case XK_multiply:
    case XK_KP_Multiply:
      return VKEY_MULTIPLY;
    case XK_KP_Add:
      return VKEY_ADD;
    case XK_KP_Separator:
      return VKEY_SEPARATOR;
    case XK_KP_Subtract:
      return VKEY_SUBTRACT;
    case XK_KP_Decimal:
      return VKEY_DECIMAL;
    case XK_KP_Divide:
      return VKEY_DIVIDE;
    case XK_KP_Equal:
    case XK_equal:
    case XK_plus:
      return VKEY_OEM_PLUS;
    case XK_comma:
    case XK_less:
      return VKEY_OEM_COMMA;
    case XK_minus:
    case XK_underscore:
      return VKEY_OEM_MINUS;
    case XK_greater:
    case XK_period:
      return VKEY_OEM_PERIOD;
    case XK_colon:
    case XK_semicolon:
      return VKEY_OEM_1;
    case XK_question:
    case XK_slash:
      return VKEY_OEM_2;
    case XK_asciitilde:
    case XK_quoteleft:
      return VKEY_OEM_3;
    case XK_bracketleft:
    case XK_braceleft:
      return VKEY_OEM_4;
    case XK_backslash:
    case XK_bar:
      return VKEY_OEM_5;
    case XK_bracketright:
    case XK_braceright:
      return VKEY_OEM_6;
    case XK_quoteright:
    case XK_quotedbl:
      return VKEY_OEM_7;
    case XK_ISO_Level5_Shift:
      return VKEY_OEM_8;
    case XK_Shift_L:
    case XK_Shift_R:
      return VKEY_SHIFT;
    case XK_Control_L:
    case XK_Control_R:
      return VKEY_CONTROL;
    case XK_Meta_L:
    case XK_Meta_R:
    case XK_Alt_L:
    case XK_Alt_R:
      return VKEY_MENU;
    case XK_ISO_Level3_Shift:
      return VKEY_ALTGR;
    case XK_Multi_key:
      return VKEY_COMPOSE;
    case XK_Pause:
      return VKEY_PAUSE;
    case XK_Caps_Lock:
      return VKEY_CAPITAL;
    case XK_Num_Lock:
      return VKEY_NUMLOCK;
    case XK_Scroll_Lock:
      return VKEY_SCROLL;
    case XK_Select:
      return VKEY_SELECT;
    case XK_Print:
      return VKEY_PRINT;
    case XK_Execute:
      return VKEY_EXECUTE;
    case XK_Insert:
    case XK_KP_Insert:
      return VKEY_INSERT;
    case XK_Help:
      return VKEY_HELP;
    case XK_Super_L:
      return VKEY_LWIN;
    case XK_Super_R:
      return VKEY_RWIN;
    case XK_Menu:
      return VKEY_APPS;
    case XK_F1:
    case XK_F2:
    case XK_F3:
    case XK_F4:
    case XK_F5:
    case XK_F6:
    case XK_F7:
    case XK_F8:
    case XK_F9:
    case XK_F10:
    case XK_F11:
    case XK_F12:
    case XK_F13:
    case XK_F14:
    case XK_F15:
    case XK_F16:
    case XK_F17:
    case XK_F18:
    case XK_F19:
    case XK_F20:
    case XK_F21:
    case XK_F22:
    case XK_F23:
    case XK_F24:
      return static_cast<KeyboardCode>(VKEY_F1 + (keysym - XK_F1));
    case XK_KP_F1:
    case XK_KP_F2:
    case XK_KP_F3:
    case XK_KP_F4:
      return static_cast<KeyboardCode>(VKEY_F1 + (keysym - XK_KP_F1));

    case XK_guillemotleft:
    case XK_guillemotright:
    case XK_degree:
      // In the case of canadian multilingual keyboard layout, VKEY_OEM_102 is
      // assigned to ugrave key.
    case XK_ugrave:
    case XK_Ugrave:
    case XK_brokenbar:
      return VKEY_OEM_102;  // international backslash key in 102 keyboard.

      // When evdev is in use, /usr/share/X11/xkb/symbols/inet maps F13-18 keys
      // to the special XF86XK symbols to support Microsoft Ergonomic keyboards:
      // https://bugs.freedesktop.org/show_bug.cgi?id=5783
      // In Chrome, we map these X key symbols back to F13-18 since we don't have
      // VKEYs for these XF86XK symbols.
    case XF86XK_Tools:
      return VKEY_F13;
    case XF86XK_Launch5:
      return VKEY_F14;
    case XF86XK_Launch6:
      return VKEY_F15;
    case XF86XK_Launch7:
      return VKEY_F16;
    case XF86XK_Launch8:
      return VKEY_F17;
    case XF86XK_Launch9:
      return VKEY_F18;
    case XF86XK_Refresh:
    case XF86XK_History:
    case XF86XK_OpenURL:
    case XF86XK_AddFavorite:
    case XF86XK_Go:
    case XF86XK_ZoomIn:
    case XF86XK_ZoomOut:
      // ui::AcceleratorGtk tries to convert the XF86XK_ keysyms on Chrome
      // startup. It's safe to return VKEY_UNKNOWN here since ui::AcceleratorGtk
      // also checks a Gdk keysym. http://crbug.com/109843
      return VKEY_UNKNOWN;
      // For supporting multimedia buttons on a USB keyboard.
    case XF86XK_Back:
      return VKEY_BROWSER_BACK;
    case XF86XK_Forward:
      return VKEY_BROWSER_FORWARD;
    case XF86XK_Reload:
      return VKEY_BROWSER_REFRESH;
    case XF86XK_Stop:
      return VKEY_BROWSER_STOP;
    case XF86XK_Search:
      return VKEY_BROWSER_SEARCH;
    case XF86XK_Favorites:
      return VKEY_BROWSER_FAVORITES;
    case XF86XK_HomePage:
      return VKEY_BROWSER_HOME;
    case XF86XK_AudioMute:
      return VKEY_VOLUME_MUTE;
    case XF86XK_AudioLowerVolume:
      return VKEY_VOLUME_DOWN;
    case XF86XK_AudioRaiseVolume:
      return VKEY_VOLUME_UP;
    case XF86XK_AudioNext:
      return VKEY_MEDIA_NEXT_TRACK;
    case XF86XK_AudioPrev:
      return VKEY_MEDIA_PREV_TRACK;
    case XF86XK_AudioStop:
      return VKEY_MEDIA_STOP;
    case XF86XK_AudioPlay:
      return VKEY_MEDIA_PLAY_PAUSE;
    case XF86XK_Mail:
      return VKEY_MEDIA_LAUNCH_MAIL;
    case XF86XK_LaunchA:  // F3 on an Apple keyboard.
      return VKEY_MEDIA_LAUNCH_APP1;
    case XF86XK_LaunchB:  // F4 on an Apple keyboard.
    case XF86XK_Calculator:
      return VKEY_MEDIA_LAUNCH_APP2;
    case XF86XK_WLAN:
      return VKEY_WLAN;
    case XF86XK_PowerOff:
      return VKEY_POWER;
    case XF86XK_MonBrightnessDown:
      return VKEY_BRIGHTNESS_DOWN;
    case XF86XK_MonBrightnessUp:
      return VKEY_BRIGHTNESS_UP;
    case XF86XK_KbdBrightnessDown:
      return VKEY_KBD_BRIGHTNESS_DOWN;
    case XF86XK_KbdBrightnessUp:
      return VKEY_KBD_BRIGHTNESS_UP;

      // TODO(sad): some keycodes are still missing.
  }
  return VKEY_UNKNOWN;
}

// From content/browser/renderer_host/input/web_input_event_util_posix.cc.
KeyboardCode GetWindowsKeyCodeWithoutLocation(KeyboardCode key_code) {
  switch (key_code) {
    case VKEY_LCONTROL:
    case VKEY_RCONTROL:
      return VKEY_CONTROL;
    case VKEY_LSHIFT:
    case VKEY_RSHIFT:
      return VKEY_SHIFT;
    case VKEY_LMENU:
    case VKEY_RMENU:
      return VKEY_MENU;
    default:
      return key_code;
  }
}

// From content/browser/renderer_host/input/web_input_event_builders_gtk.cc.
// Gets the corresponding control character of a specified key code. See:
// http://en.wikipedia.org/wiki/Control_characters
// We emulate Windows behavior here.
int GetControlCharacter(KeyboardCode windows_key_code, bool shift) {
  if (windows_key_code >= VKEY_A && windows_key_code <= VKEY_Z) {
    // ctrl-A ~ ctrl-Z map to \x01 ~ \x1A
    return windows_key_code - VKEY_A + 1;
  }
  if (shift) {
    // following graphics chars require shift key to input.
    switch (windows_key_code) {
      // ctrl-@ maps to \x00 (Null byte)
      case VKEY_2:
        return 0;
        // ctrl-^ maps to \x1E (Record separator, Information separator two)
      case VKEY_6:
        return 0x1E;
        // ctrl-_ maps to \x1F (Unit separator, Information separator one)
      case VKEY_OEM_MINUS:
        return 0x1F;
        // Returns 0 for all other keys to avoid inputting unexpected chars.
      default:
        return 0;
    }
  } else {
    switch (windows_key_code) {
      // ctrl-[ maps to \x1B (Escape)
      case VKEY_OEM_4:
        return 0x1B;
        // ctrl-\ maps to \x1C (File separator, Information separator four)
      case VKEY_OEM_5:
        return 0x1C;
        // ctrl-] maps to \x1D (Group separator, Information separator three)
      case VKEY_OEM_6:
        return 0x1D;
        // ctrl-Enter maps to \x0A (Line feed)
      case VKEY_RETURN:
        return 0x0A;
        // Returns 0 for all other keys to avoid inputting unexpected chars.
      default:
        return 0;
    }
  }
}

#endif

}  // namespace

bool javaKeyEventToCef(JNIEnv* env,
                       jobject jKeyEvent,
                       CefKeyEventAttributes* result) {
  if (!result) {
    return false;
  }

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

#if defined(OS_WIN)
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

#if defined(OS_MAC)
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

#if defined(OS_LINUX)
  int keyCode, keyLocation;
  if (!CallJNIMethodI_V(env, cls, jKeyEvent, "getKeyCode", &keyCode) ||
      !CallJNIMethodI_V(env, cls, jKeyEvent, "getKeyLocation", &keyLocation)) {
    return false;
  }

  result->native_key_code =
      JavaKeyCode2X11(env, static_cast<jclass>(cls), keyCode);

  KeyboardCode windows_key_code =
      KeyboardCodeFromXKeysym(result->native_key_code);
  result->windows_key_code = GetWindowsKeyCodeWithoutLocation(windows_key_code);

  if (result->modifiers & CefModifiers::EVENTFLAG_ALT_DOWN)
    result->is_system_key = true;

  if (windows_key_code == VKEY_RETURN) {
    // We need to treat the enter key as a key press of character \r.  This
    // is apparently just how webkit handles it and what it expects.
    result->unmodified_character = '\r';
  } else {
    result->unmodified_character = key_char != '\n' ? key_char : '\r';
  }

  // If ctrl key is pressed down, then control character shall be input.
  if (result->modifiers & CefModifiers::EVENTFLAG_CONTROL_DOWN) {
    result->character = GetControlCharacter(
        windows_key_code, result->modifiers & EVENTFLAG_SHIFT_DOWN);
  } else {
    result->character = result->unmodified_character;
  }
#endif

  return true;
}

}  // namespace jcef_keyboard_utils
