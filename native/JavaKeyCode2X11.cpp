#include <X11/keysym.h>
#include <jni.h>

namespace {

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

}  // namespace

#define JNI_STATIC(name) _static_##name

#define JNI_STATIC_DEFINE_INT_RV(env, cls, name, rv)                        \
  static int JNI_STATIC(name) = -1;                                         \
  if (JNI_STATIC(name) == -1 && !GetJNIFieldStaticInt( \
                                    env, cls, #name, &JNI_STATIC(name))) {  \
    return rv;                                                              \
  }

int JavaKeyCode2X11(JNIEnv* env,
                    jclass cls /*KeyEvent*/,
                    int keycode) {
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ENTER, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BACK_SPACE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_TAB, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CANCEL, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CLEAR, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SHIFT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CONTROL, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ALT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PAUSE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CAPS_LOCK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ESCAPE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SPACE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PAGE_UP, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PAGE_DOWN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_END, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_HOME, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_LEFT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_UP, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_RIGHT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DOWN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_COMMA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_MINUS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PERIOD, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SLASH, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_0, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_1, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_2, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_3, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_4, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_5, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_6, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_7, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_8, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_9, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SEMICOLON, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_EQUALS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_A, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_B, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_C, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_D, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_E, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_G, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_H, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_I, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_J, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_K, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_L, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_M, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_N, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_O, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_P, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_Q, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_R, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_S, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_T, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_U, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_V, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_W, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_X, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_Y, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_Z, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_OPEN_BRACKET, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BACK_SLASH, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CLOSE_BRACKET, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD0, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD1, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD2, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD3, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD4, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD5, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD6, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD7, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD8, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMPAD9, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_MULTIPLY, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ADD, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SEPARATER, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SEPARATOR, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SUBTRACT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DECIMAL, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DIVIDE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DELETE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUM_LOCK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_SCROLL_LOCK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F1, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F2, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F3, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F4, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F5, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F6, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F7, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F8, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F9, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F10, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F11, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F12, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F13, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F14, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F15, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F16, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F17, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F18, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F19, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F20, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F21, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F22, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F23, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_F24, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PRINTSCREEN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_INSERT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_HELP, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_META, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BACK_QUOTE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_QUOTE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KP_UP, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KP_DOWN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KP_LEFT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KP_RIGHT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_GRAVE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_ACUTE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_CIRCUMFLEX, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_TILDE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_MACRON, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_BREVE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_ABOVEDOT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_DIAERESIS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_ABOVERING, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_DOUBLEACUTE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_CARON, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_CEDILLA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_OGONEK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_IOTA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_VOICED_SOUND, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DEAD_SEMIVOICED_SOUND, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_AMPERSAND, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ASTERISK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_QUOTEDBL, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_LESS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_GREATER, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BRACELEFT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BRACERIGHT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_AT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_COLON, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CIRCUMFLEX, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_DOLLAR, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_EURO_SIGN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_EXCLAMATION_MARK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_INVERTED_EXCLAMATION_MARK,
                           XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_LEFT_PARENTHESIS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NUMBER_SIGN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PLUS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_RIGHT_PARENTHESIS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_UNDERSCORE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_WINDOWS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CONTEXT_MENU, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_FINAL, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CONVERT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_NONCONVERT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ACCEPT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_MODECHANGE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KANA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KANJI, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ALPHANUMERIC, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KATAKANA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_HIRAGANA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_FULL_WIDTH, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_HALF_WIDTH, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ROMAN_CHARACTERS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ALL_CANDIDATES, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PREVIOUS_CANDIDATE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CODE_INPUT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_JAPANESE_KATAKANA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_JAPANESE_HIRAGANA, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_JAPANESE_ROMAN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_KANA_LOCK, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_INPUT_METHOD_ON_OFF, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_CUT, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_COPY, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PASTE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_UNDO, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_AGAIN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_FIND, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_PROPS, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_STOP, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_COMPOSE, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_ALT_GRAPH, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_BEGIN, XK_VoidSymbol);
  JNI_STATIC_DEFINE_INT_RV(env, cls, VK_UNDEFINED, XK_VoidSymbol);

  if (keycode >= JNI_STATIC(VK_A) && keycode <= JNI_STATIC(VK_Z))
    return XK_a + (keycode - JNI_STATIC(VK_A));

  if (keycode >= JNI_STATIC(VK_0) && keycode <= JNI_STATIC(VK_9))
    return XK_0 + (keycode - JNI_STATIC(VK_0));
  if (keycode >= JNI_STATIC(VK_NUMPAD0) && keycode <= JNI_STATIC(VK_NUMPAD9))
    return XK_KP_0 + (keycode - JNI_STATIC(VK_NUMPAD0));

  if (keycode >= JNI_STATIC(VK_F1) && keycode <= JNI_STATIC(VK_F12))
    return XK_F1 + (keycode - JNI_STATIC(VK_F1));

  if (keycode == JNI_STATIC(VK_ENTER))
    return XK_Return;
  else if (keycode == JNI_STATIC(VK_BACK_SPACE))
    return XK_BackSpace;
  else if (keycode == JNI_STATIC(VK_TAB))
    return XK_Tab;
  else if (keycode == JNI_STATIC(VK_CANCEL))
    return XK_Cancel;
  else if (keycode == JNI_STATIC(VK_CLEAR))
    return XK_Clear;

  else if (keycode == JNI_STATIC(VK_SHIFT))
    return XK_Shift_L;
  else if (keycode == JNI_STATIC(VK_CONTROL))
    return XK_Control_L;
  else if (keycode == JNI_STATIC(VK_ALT))
    return XK_Alt_L;

  else if (keycode == JNI_STATIC(VK_PAUSE))
    return XK_Pause;
  else if (keycode == JNI_STATIC(VK_CAPS_LOCK))
    return XK_Caps_Lock;
  else if (keycode == JNI_STATIC(VK_ESCAPE))
    return XK_Escape;
  else if (keycode == JNI_STATIC(VK_SPACE))
    return XK_space;
  else if (keycode == JNI_STATIC(VK_PAGE_UP))
    return XK_Page_Up;
  else if (keycode == JNI_STATIC(VK_PAGE_DOWN))
    return XK_Page_Down;
  else if (keycode == JNI_STATIC(VK_END))
    return XK_End;
  else if (keycode == JNI_STATIC(VK_HOME))
    return XK_Home;

  else if (keycode == JNI_STATIC(VK_LEFT))
    return XK_Left;
  else if (keycode == JNI_STATIC(VK_KP_LEFT))
    return XK_KP_Left;
  else if (keycode == JNI_STATIC(VK_UP))
    return XK_Up;
  else if (keycode == JNI_STATIC(VK_KP_UP))
    return XK_KP_Up;
  else if (keycode == JNI_STATIC(VK_RIGHT))
    return XK_Right;
  else if (keycode == JNI_STATIC(VK_KP_RIGHT))
    return XK_KP_Right;
  else if (keycode == JNI_STATIC(VK_DOWN))
    return XK_Down;
  else if (keycode == JNI_STATIC(VK_KP_DOWN))
    return XK_KP_Down;

  else if (keycode == JNI_STATIC(VK_COMMA))
    return XK_comma;
  else if (keycode == JNI_STATIC(VK_MINUS))
    return XK_minus;
  else if (keycode == JNI_STATIC(VK_PERIOD))
    return XK_period;
  else if (keycode == JNI_STATIC(VK_SLASH))
    return XK_slash;

  else if (keycode == JNI_STATIC(VK_SEMICOLON))
    return XK_semicolon;
  else if (keycode == JNI_STATIC(VK_EQUALS))
    return XK_equal;

  else if (keycode == JNI_STATIC(VK_OPEN_BRACKET))
    return XK_bracketleft;
  else if (keycode == JNI_STATIC(VK_BACK_SLASH))
    return XK_backslash;
  else if (keycode == JNI_STATIC(VK_CLOSE_BRACKET))
    return XK_bracketright;

  else if (keycode == JNI_STATIC(VK_MULTIPLY))
    return XK_multiply;
  else if (keycode == JNI_STATIC(VK_ADD))
    return XK_KP_Add;
  else if (keycode == JNI_STATIC(VK_SEPARATOR))
    return XK_KP_Separator;

  else if (keycode == JNI_STATIC(VK_SUBTRACT))
    return XK_KP_Subtract;
  else if (keycode == JNI_STATIC(VK_DECIMAL))
    return XK_KP_Decimal;
  else if (keycode == JNI_STATIC(VK_DIVIDE))
    return XK_KP_Divide;
  else if (keycode == JNI_STATIC(VK_DELETE))
    return XK_Delete;
  else if (keycode == JNI_STATIC(VK_NUM_LOCK))
    return XK_Num_Lock;
  else if (keycode == JNI_STATIC(VK_SCROLL_LOCK))
    return XK_Scroll_Lock;

  else if (keycode == JNI_STATIC(VK_PRINTSCREEN))
    return XK_Print;
  else if (keycode == JNI_STATIC(VK_INSERT))
    return XK_Insert;
  else if (keycode == JNI_STATIC(VK_HELP))
    return XK_Help;
  else if (keycode == JNI_STATIC(VK_META))
    return XK_Meta_R;

  else if (keycode == JNI_STATIC(VK_BACK_QUOTE))
    return XK_quoteright;
  else if (keycode == JNI_STATIC(VK_QUOTE))
    return XK_quoteleft;

  else if (keycode == JNI_STATIC(VK_DEAD_GRAVE))
    return XK_dead_grave;
  else if (keycode == JNI_STATIC(VK_DEAD_ACUTE))
    return XK_dead_acute;
  else if (keycode == JNI_STATIC(VK_DEAD_CIRCUMFLEX))
    return XK_dead_circumflex;
  else if (keycode == JNI_STATIC(VK_DEAD_TILDE))
    return XK_dead_tilde;
  else if (keycode == JNI_STATIC(VK_DEAD_MACRON))
    return XK_dead_macron;
  else if (keycode == JNI_STATIC(VK_DEAD_BREVE))
    return XK_dead_breve;
  else if (keycode == JNI_STATIC(VK_DEAD_ABOVEDOT))
    return XK_dead_abovedot;
  else if (keycode == JNI_STATIC(VK_DEAD_DIAERESIS))
    return XK_dead_diaeresis;
  else if (keycode == JNI_STATIC(VK_DEAD_ABOVERING))
    return XK_dead_abovering;
  else if (keycode == JNI_STATIC(VK_DEAD_DOUBLEACUTE))
    return XK_dead_doubleacute;
  else if (keycode == JNI_STATIC(VK_DEAD_CARON))
    return XK_dead_caron;
  else if (keycode == JNI_STATIC(VK_DEAD_CEDILLA))
    return XK_dead_cedilla;
  else if (keycode == JNI_STATIC(VK_DEAD_OGONEK))
    return XK_dead_ogonek;
  else if (keycode == JNI_STATIC(VK_DEAD_IOTA))
    return XK_dead_iota;
  else if (keycode == JNI_STATIC(VK_DEAD_VOICED_SOUND))
    return XK_dead_voiced_sound;
  else if (keycode == JNI_STATIC(VK_DEAD_SEMIVOICED_SOUND))
    return XK_dead_semivoiced_sound;
  else if (keycode == JNI_STATIC(VK_AMPERSAND))
    return XK_ampersand;
  else if (keycode == JNI_STATIC(VK_ASTERISK))
    return XK_asterisk;
  else if (keycode == JNI_STATIC(VK_QUOTEDBL))
    return XK_quotedbl;
  else if (keycode == JNI_STATIC(VK_LESS))
    return XK_less;
  else if (keycode == JNI_STATIC(VK_GREATER))
    return XK_greater;
  else if (keycode == JNI_STATIC(VK_BRACELEFT))
    return XK_braceleft;
  else if (keycode == JNI_STATIC(VK_BRACERIGHT))
    return XK_braceright;
  else if (keycode == JNI_STATIC(VK_AT))
    return XK_at;
  else if (keycode == JNI_STATIC(VK_COLON))
    return XK_colon;
  else if (keycode == JNI_STATIC(VK_CIRCUMFLEX))
    return XK_dead_circumflex;
  else if (keycode == JNI_STATIC(VK_DOLLAR))
    return XK_dollar;
  else if (keycode == JNI_STATIC(VK_EURO_SIGN))
    return XK_EuroSign;
  else if (keycode == JNI_STATIC(VK_EXCLAMATION_MARK))
    return XK_exclamdown;
  else if (keycode == JNI_STATIC(VK_INVERTED_EXCLAMATION_MARK))
    return XK_exclam;
  else if (keycode == JNI_STATIC(VK_LEFT_PARENTHESIS))
    return XK_parenleft;
  else if (keycode == JNI_STATIC(VK_NUMBER_SIGN))
    return XK_numbersign;
  else if (keycode == JNI_STATIC(VK_PLUS))
    return XK_plus;
  else if (keycode == JNI_STATIC(VK_RIGHT_PARENTHESIS))
    return XK_parenright;
  else if (keycode == JNI_STATIC(VK_UNDERSCORE))
    return XK_underscore;

  // else if (keycode == JNI_STATIC(VK_CONTEXT_MENU)) return XK_Menu;
  // case VK_FINAL:;
  // case VK_CONVERT:;
  // case VK_NONCONVERT:;
  // case VK_ACCEPT:;
  // else if (keycode == JNI_STATIC(VK_MODECHANGE)) return XK_Mode_switch;
  // case VK_KANA:;

  else if (keycode == JNI_STATIC(VK_KANJI))
    return XK_Kanji;
  // case JNI_STATIC(VK_ALPHANUMERIC):return X11KeyCodes.;
  else if (keycode == JNI_STATIC(VK_KATAKANA))
    return XK_Katakana;
  else if (keycode == JNI_STATIC(VK_HIRAGANA))
    return XK_Hiragana;

  // case JNI_STATIC(VK_FULL_WIDTH):return X11KeyCodes.;
  // case JNI_STATIC(VK_HALF_WIDTH):return X11KeyCodes.;
  // case JNI_STATIC(VK_ROMAN_CHARACTERS):return X11KeyCodes.;
  else if (keycode == JNI_STATIC(VK_ALL_CANDIDATES))
    return XK_MultipleCandidate;
  else if (keycode == JNI_STATIC(VK_PREVIOUS_CANDIDATE))
    return XK_PreviousCandidate;
  else if (keycode == JNI_STATIC(VK_CODE_INPUT))
    return XK_Codeinput;
  // case JNI_STATIC(VK_JAPANESE_KATAKANA):return X11KeyCodes.;
  else if (keycode == JNI_STATIC(VK_JAPANESE_HIRAGANA))
    return XK_Hiragana;
  // case JNI_STATIC(VK_JAPANESE_ROMAN):return X11KeyCodes.;
  else if (keycode == JNI_STATIC(VK_KANA_LOCK))
    return XK_Kana_Lock;
  // case JNI_STATIC(VK_INPUT_METHOD_ON_OFF):return X11KeyCodes.;
  // case JNI_STATIC(VK_CUT):return X11KeyCodes.;
  // case JNI_STATIC(VK_COPY):return X11KeyCodes.;
  // case JNI_STATIC(VK_PASTE):return X11KeyCodes.;
  // case JNI_STATIC(VK_UNDO):return X11KeyCodes.;
  // case JNI_STATIC(VK_AGAIN):return X11KeyCodes.;
  else if (keycode == JNI_STATIC(VK_FIND))
    return XK_Find;
  // case JNI_STATIC(VK_PROPS):return X11KeyCodes.;
  // case JNI_STATIC(VK_STOP):return X11KeyCodes.;
  // case JNI_STATIC(VK_COMPOSE):return X11KeyCodes.;
  // case JNI_STATIC(VK_ALT_GRAPH):return X11KeyCodes.;
  else if (keycode == JNI_STATIC(VK_BEGIN))
    return XK_Begin;

  return 0 /*VK_UNDEFINED*/;
}
