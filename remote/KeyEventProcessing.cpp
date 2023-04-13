
#include <Carbon/Carbon.h>

#include "include/cef_base.h"

namespace {
//
// Constants from KeyEvent.java
// NOTE: wasn't modified last xx years and it seems that we can
// just copy them (jcef reads them from class via JNI)
//
// TODO: write key-event tests for various keys
//
const int KEY_FIRST = 400;
const int KEY_LAST = 402;
const int KEY_TYPED = KEY_FIRST;
const int KEY_PRESSED = 1 + KEY_FIRST;   // Event.KEY_PRESS
const int KEY_RELEASED = 2 + KEY_FIRST;  // Event.KEY_RELEASE
const int VK_ENTER = '\n';
const int VK_BACK_SPACE = '\b';
const int VK_TAB = '\t';
const int VK_CANCEL = 0x03;
const int VK_CLEAR = 0x0C;
const int VK_SHIFT = 0x10;
const int VK_CONTROL = 0x11;
const int VK_ALT = 0x12;
const int VK_PAUSE = 0x13;
const int VK_CAPS_LOCK = 0x14;
const int VK_ESCAPE = 0x1B;
const int VK_SPACE = 0x20;
const int VK_PAGE_UP = 0x21;
const int VK_PAGE_DOWN = 0x22;
const int VK_END = 0x23;
const int VK_HOME = 0x24;
const int VK_LEFT = 0x25;
const int VK_UP = 0x26;
const int VK_RIGHT = 0x27;
const int VK_DOWN = 0x28;
const int VK_COMMA = 0x2C;
const int VK_MINUS = 0x2D;
const int VK_PERIOD = 0x2E;
const int VK_SLASH = 0x2F;
const int VK_0 = 0x30;
const int VK_1 = 0x31;
const int VK_2 = 0x32;
const int VK_3 = 0x33;
const int VK_4 = 0x34;
const int VK_5 = 0x35;
const int VK_6 = 0x36;
const int VK_7 = 0x37;
const int VK_8 = 0x38;
const int VK_9 = 0x39;
const int VK_SEMICOLON = 0x3B;
const int VK_EQUALS = 0x3D;
const int VK_A = 0x41;
const int VK_B = 0x42;
const int VK_C = 0x43;
const int VK_D = 0x44;
const int VK_E = 0x45;
const int VK_F = 0x46;
const int VK_G = 0x47;
const int VK_H = 0x48;
const int VK_I = 0x49;
const int VK_J = 0x4A;
const int VK_K = 0x4B;
const int VK_L = 0x4C;
const int VK_M = 0x4D;
const int VK_N = 0x4E;
const int VK_O = 0x4F;
const int VK_P = 0x50;
const int VK_Q = 0x51;
const int VK_R = 0x52;
const int VK_S = 0x53;
const int VK_T = 0x54;
const int VK_U = 0x55;
const int VK_V = 0x56;
const int VK_W = 0x57;
const int VK_X = 0x58;
const int VK_Y = 0x59;
const int VK_Z = 0x5A;
const int VK_OPEN_BRACKET = 0x5B;
const int VK_BACK_SLASH = 0x5C;
const int VK_CLOSE_BRACKET = 0x5D;
const int VK_NUMPAD0 = 0x60;
const int VK_NUMPAD1 = 0x61;
const int VK_NUMPAD2 = 0x62;
const int VK_NUMPAD3 = 0x63;
const int VK_NUMPAD4 = 0x64;
const int VK_NUMPAD5 = 0x65;
const int VK_NUMPAD6 = 0x66;
const int VK_NUMPAD7 = 0x67;
const int VK_NUMPAD8 = 0x68;
const int VK_NUMPAD9 = 0x69;
const int VK_MULTIPLY = 0x6A;
const int VK_ADD = 0x6B;
const int VK_SEPARATER = 0x6C;
const int VK_SEPARATOR = VK_SEPARATER;
const int VK_SUBTRACT = 0x6D;
const int VK_DECIMAL = 0x6E;
const int VK_DIVIDE = 0x6F;
const int VK_DELETE = 0x7F;
const int VK_NUM_LOCK = 0x90;
const int VK_SCROLL_LOCK = 0x91;
const int VK_F1 = 0x70;
const int VK_F2 = 0x71;
const int VK_F3 = 0x72;
const int VK_F4 = 0x73;
const int VK_F5 = 0x74;
const int VK_F6 = 0x75;
const int VK_F7 = 0x76;
const int VK_F8 = 0x77;
const int VK_F9 = 0x78;
const int VK_F10 = 0x79;
const int VK_F11 = 0x7A;
const int VK_F12 = 0x7B;
const int VK_F13 = 0xF000;
const int VK_F14 = 0xF001;
const int VK_F15 = 0xF002;
const int VK_F16 = 0xF003;
const int VK_F17 = 0xF004;
const int VK_F18 = 0xF005;
const int VK_F19 = 0xF006;
const int VK_F20 = 0xF007;
const int VK_F21 = 0xF008;
const int VK_F22 = 0xF009;
const int VK_F23 = 0xF00A;
const int VK_F24 = 0xF00B;
const int VK_PRINTSCREEN = 0x9A;
const int VK_INSERT = 0x9B;
const int VK_HELP = 0x9C;
const int VK_META = 0x9D;
const int VK_BACK_QUOTE = 0xC0;
const int VK_QUOTE = 0xDE;
const int VK_KP_UP = 0xE0;
const int VK_KP_DOWN = 0xE1;
const int VK_KP_LEFT = 0xE2;
const int VK_KP_RIGHT = 0xE3;
const int VK_DEAD_GRAVE = 0x80;
const int VK_DEAD_ACUTE = 0x81;
const int VK_DEAD_CIRCUMFLEX = 0x82;
const int VK_DEAD_TILDE = 0x83;
const int VK_DEAD_MACRON = 0x84;
const int VK_DEAD_BREVE = 0x85;
const int VK_DEAD_ABOVEDOT = 0x86;
const int VK_DEAD_DIAERESIS = 0x87;
const int VK_DEAD_ABOVERING = 0x88;
const int VK_DEAD_DOUBLEACUTE = 0x89;
const int VK_DEAD_CARON = 0x8a;
const int VK_DEAD_CEDILLA = 0x8b;
const int VK_DEAD_OGONEK = 0x8c;
const int VK_DEAD_IOTA = 0x8d;
const int VK_DEAD_VOICED_SOUND = 0x8e;
const int VK_DEAD_SEMIVOICED_SOUND = 0x8f;
const int VK_AMPERSAND = 0x96;
const int VK_ASTERISK = 0x97;
const int VK_QUOTEDBL = 0x98;
const int VK_LESS = 0x99;
const int VK_GREATER = 0xa0;
const int VK_BRACELEFT = 0xa1;
const int VK_BRACERIGHT = 0xa2;
const int VK_AT = 0x0200;
const int VK_COLON = 0x0201;
const int VK_CIRCUMFLEX = 0x0202;
const int VK_DOLLAR = 0x0203;
const int VK_EURO_SIGN = 0x0204;
const int VK_EXCLAMATION_MARK = 0x0205;
const int VK_INVERTED_EXCLAMATION_MARK = 0x0206;
const int VK_LEFT_PARENTHESIS = 0x0207;
const int VK_NUMBER_SIGN = 0x0208;
const int VK_PLUS = 0x0209;
const int VK_RIGHT_PARENTHESIS = 0x020A;
const int VK_UNDERSCORE = 0x020B;
const int VK_WINDOWS = 0x020C;
const int VK_CONTEXT_MENU = 0x020D;
const int VK_FINAL = 0x0018;
const int VK_CONVERT = 0x001C;
const int VK_NONCONVERT = 0x001D;
const int VK_ACCEPT = 0x001E;
const int VK_MODECHANGE = 0x001F;
const int VK_KANA = 0x0015;
const int VK_KANJI = 0x0019;
const int VK_ALPHANUMERIC = 0x00F0;
const int VK_KATAKANA = 0x00F1;
const int VK_HIRAGANA = 0x00F2;
const int VK_FULL_WIDTH = 0x00F3;
const int VK_HALF_WIDTH = 0x00F4;
const int VK_ROMAN_CHARACTERS = 0x00F5;
const int VK_ALL_CANDIDATES = 0x0100;
const int VK_PREVIOUS_CANDIDATE = 0x0101;
const int VK_CODE_INPUT = 0x0102;
const int VK_JAPANESE_KATAKANA = 0x0103;
const int VK_JAPANESE_HIRAGANA = 0x0104;
const int VK_JAPANESE_ROMAN = 0x0105;
const int VK_KANA_LOCK = 0x0106;
const int VK_INPUT_METHOD_ON_OFF = 0x0107;
const int VK_CUT = 0xFFD1;
const int VK_COPY = 0xFFCD;
const int VK_PASTE = 0xFFCF;
const int VK_UNDO = 0xFFCB;
const int VK_AGAIN = 0xFFC9;
const int VK_FIND = 0xFFD0;
const int VK_PROPS = 0xFFCA;
const int VK_STOP = 0xFFC8;
const int VK_COMPOSE = 0xFF20;
const int VK_ALT_GRAPH = 0xFF7E;
const int VK_BEGIN = 0xFF58;
const int VK_UNDEFINED = 0x0;
const char CHAR_UNDEFINED = 0xFFFF;
const int KEY_LOCATION_UNKNOWN = 0;
const int KEY_LOCATION_STANDARD = 1;
const int KEY_LOCATION_LEFT = 2;
const int KEY_LOCATION_RIGHT = 3;
const int KEY_LOCATION_NUMPAD = 4;

//
// Constants from InputEvent.java
//

const int SHIFT_DOWN_MASK = 1 << 6;
const int CTRL_DOWN_MASK = 1 << 7;
const int META_DOWN_MASK = 1 << 8;
const int ALT_DOWN_MASK = 1 << 9;
const int BUTTON1_DOWN_MASK = 1 << 10;
const int BUTTON2_DOWN_MASK = 1 << 11;
const int BUTTON3_DOWN_MASK = 1 << 12;
const int ALT_GRAPH_DOWN_MASK = 1 << 13;

//
// NOTE: next code is modified copy-paste of code from CefBrowser_N.cpp (see SendKeyEvent)
//

#if defined(OS_MAC)
// A convenient array for getting symbol characters on the number keys.
const char kShiftCharsForNumberKeys[] = ")!@#$%^&*(";

// Convert an ANSI character to a Mac key code.
int GetMacKeyCodeFromChar(int key_char) {
  switch (key_char) {
    case ' ':
      return kVK_Space;
    case '\n':
      return kVK_Return;

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
#endif  // defined(OS_MAC)

int GetCefModifiers(int modifiers) {
  int cef_modifiers = 0;
  if (modifiers & (ALT_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_ALT_DOWN;
  if (modifiers & (BUTTON1_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_LEFT_MOUSE_BUTTON;
  if (modifiers & (BUTTON2_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_MIDDLE_MOUSE_BUTTON;
  if (modifiers & (BUTTON3_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_RIGHT_MOUSE_BUTTON;
  if (modifiers & (CTRL_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_CONTROL_DOWN;
  if (modifiers & (META_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_COMMAND_DOWN;
  if (modifiers & (SHIFT_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_SHIFT_DOWN;

  return cef_modifiers;
}

} // anon namespace

void processKeyEvent(
    CefKeyEvent & cef_event,
    int event_type, // event.getID()
    int modifiers,  // event.getModifiersEx()
    char16 key_char, // event.getKeyChar()
    long scanCode,   // event.scancode, windows only
    int key_code   // event.getKeyCode()
) {
  cef_event.modifiers = GetCefModifiers(modifiers);

#if defined(OS_WIN)
  BYTE VkCode = LOBYTE(MapVirtualKey(scanCode, MAPVK_VSC_TO_VK));
  cef_event.native_key_code = (scanCode << 16) |  // key scan code
                              1;                  // key repeat count
#elif defined(OS_LINUX) || defined(OS_MAC)
#if defined(OS_LINUX)
  cef_event.native_key_code = JavaKeyCode2X11(env, &cls, key_code);

  KeyboardCode windows_key_code =
      KeyboardCodeFromXKeysym(cef_event.native_key_code);
  cef_event.windows_key_code =
      GetWindowsKeyCodeWithoutLocation(windows_key_code);

  if (cef_event.modifiers & EVENTFLAG_ALT_DOWN)
    cef_event.is_system_key = true;

  if (windows_key_code == VKEY_RETURN) {
    // We need to treat the enter key as a key press of character \r.  This
    // is apparently just how webkit handles it and what it expects.
    cef_event.unmodified_character = '\r';
  } else {
    cef_event.unmodified_character = key_char != '\n' ? key_char : '\r';
  }

  // If ctrl key is pressed down, then control character shall be input.
  if (cef_event.modifiers & EVENTFLAG_CONTROL_DOWN) {
    cef_event.character = GetControlCharacter(
        windows_key_code, cef_event.modifiers & EVENTFLAG_SHIFT_DOWN);
  } else {
    cef_event.character = cef_event.unmodified_character;
  }
#elif defined(OS_MAC)
  if (key_code == (VK_BACK_SPACE)) {
    cef_event.native_key_code = kVK_Delete;
    cef_event.unmodified_character = kBackspaceCharCode;
  } else if (key_code == (VK_DELETE)) {
    cef_event.native_key_code = kVK_ForwardDelete;
    cef_event.unmodified_character = kDeleteCharCode;
  } else if (key_code == (VK_DOWN)) {
    cef_event.native_key_code = kVK_DownArrow;
    cef_event.unmodified_character = /* NSDownArrowFunctionKey */ 0xF701;
  } else if (key_code == (VK_ENTER)) {
    cef_event.native_key_code = kVK_Return;
    cef_event.unmodified_character = kReturnCharCode;
  } else if (key_code == (VK_ESCAPE)) {
    cef_event.native_key_code = kVK_Escape;
    cef_event.unmodified_character = kEscapeCharCode;
  } else if (key_code == (VK_LEFT)) {
    cef_event.native_key_code = kVK_LeftArrow;
    cef_event.unmodified_character = /* NSLeftArrowFunctionKey */ 0xF702;
  } else if (key_code == (VK_RIGHT)) {
    cef_event.native_key_code = kVK_RightArrow;
    cef_event.unmodified_character = /* NSRightArrowFunctionKey */ 0xF703;
  } else if (key_code == (VK_TAB)) {
    cef_event.native_key_code = kVK_Tab;
    cef_event.unmodified_character = kTabCharCode;
  } else if (key_code == (VK_UP)) {
    cef_event.native_key_code = kVK_UpArrow;
    cef_event.unmodified_character = /* NSUpArrowFunctionKey */ 0xF700;
  } else {
    cef_event.native_key_code = GetMacKeyCodeFromChar(key_char);
    if (cef_event.native_key_code == -1)
      cef_event.native_key_code = 0;

    if (cef_event.native_key_code == kVK_Return) {
      cef_event.unmodified_character = kReturnCharCode;
    } else {
      cef_event.unmodified_character = key_char;
    }
  }

  cef_event.character = cef_event.unmodified_character;

  // Fill in |character| according to flags.
  if (cef_event.modifiers & EVENTFLAG_SHIFT_DOWN) {
    if (key_char >= '0' && key_char <= '9') {
      cef_event.character = kShiftCharsForNumberKeys[key_char - '0'];
    } else if (key_char >= 'A' && key_char <= 'Z') {
      cef_event.character = 'A' + (key_char - 'A');
    } else {
      switch (cef_event.native_key_code) {
        case kVK_ANSI_Grave:
          cef_event.character = '~';
          break;
        case kVK_ANSI_Minus:
          cef_event.character = '_';
          break;
        case kVK_ANSI_Equal:
          cef_event.character = '+';
          break;
        case kVK_ANSI_LeftBracket:
          cef_event.character = '{';
          break;
        case kVK_ANSI_RightBracket:
          cef_event.character = '}';
          break;
        case kVK_ANSI_Backslash:
          cef_event.character = '|';
          break;
        case kVK_ANSI_Semicolon:
          cef_event.character = ':';
          break;
        case kVK_ANSI_Quote:
          cef_event.character = '\"';
          break;
        case kVK_ANSI_Comma:
          cef_event.character = '<';
          break;
        case kVK_ANSI_Period:
          cef_event.character = '>';
          break;
        case kVK_ANSI_Slash:
          cef_event.character = '?';
          break;
        default:
          break;
      }
    }
  }

  // Control characters.
  if (cef_event.modifiers & EVENTFLAG_CONTROL_DOWN) {
    if (key_char >= 'A' && key_char <= 'Z')
      cef_event.character = 1 + key_char - 'A';
    else if (cef_event.native_key_code == kVK_ANSI_LeftBracket)
      cef_event.character = 27;
    else if (cef_event.native_key_code == kVK_ANSI_Backslash)
      cef_event.character = 28;
    else if (cef_event.native_key_code == kVK_ANSI_RightBracket)
      cef_event.character = 29;
  }
#endif  // defined(OS_MAC)
#endif  // defined(OS_LINUX) || defined(OS_MAC)

  if (event_type == (KEY_PRESSED)) {
#if defined(OS_WIN)
    cef_event.windows_key_code = VkCode;
#endif
    cef_event.type = KEYEVENT_RAWKEYDOWN;
  } else if (event_type == (KEY_RELEASED)) {
#if defined(OS_WIN)
    cef_event.windows_key_code = VkCode;
    // bits 30 and 31 should always be 1 for WM_KEYUP
    cef_event.native_key_code |= 0xC0000000;
#endif
    cef_event.type = KEYEVENT_KEYUP;
  } else if (event_type == (KEY_TYPED)) {
#if defined(OS_WIN)
    cef_event.windows_key_code = key_char == '\n' ? '\r' : key_char;
#endif
    cef_event.type = KEYEVENT_CHAR;
  }
}