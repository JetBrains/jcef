package tests.keyboard;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Scenario {
    public String name;
    public String comments;
    public List<EventDataJava> eventsJava;
    public List<EventDataJS> eventsJSExpected;

    public Scenario(String name, List<EventDataJava> events) {
        this(name, "", events, new ArrayList<>());
    }

    public Scenario(String name, String comments, List<EventDataJava> eventsJava, List<EventDataJS> eventsJSExpected) {
        this.name = name;
        this.comments = comments;
        this.eventsJava = eventsJava;
        this.eventsJSExpected = eventsJSExpected;
    }

    @Override
    public String toString() {
        return name + "(" + eventsJava.size() + " events)";
    }

    public static class EventDataJava {
        static final Map<Integer, String> ID_TO_TYPE = Map.of(
                KeyEvent.KEY_PRESSED, "KEY_PRESSED",
                KeyEvent.KEY_TYPED, "KEY_TYPED",
                KeyEvent.KEY_RELEASED, "KEY_RELEASED"
        );
        static final Map<Integer, String> KEY_LOCATION_TO_STRING = Map.of(
                KeyEvent.KEY_LOCATION_UNKNOWN, "KEY_LOCATION_UNKNOWN",
                KeyEvent.KEY_LOCATION_STANDARD, "KEY_LOCATION_STANDARD",
                KeyEvent.KEY_LOCATION_LEFT, "KEY_LOCATION_LEFT",
                KeyEvent.KEY_LOCATION_RIGHT, "KEY_LOCATION_RIGHT",
                KeyEvent.KEY_LOCATION_NUMPAD, "KEY_LOCATION_NUMPAD"
        );
        public  int id;
        public int modifiers;
        public int keyCode;
        public char keyChar;
        public int keyLocation;
        private long rawCode = 0;
        private long primaryLevelUnicode = 0;
        private long scancode = 0; // for MS Windows only
        private long extendedKeyCode = 0;

        EventDataJava(KeyEvent e) {
            id = e.getID();
            modifiers = e.getModifiersEx();
            keyCode = e.getKeyCode();
            keyChar = e.getKeyChar();
            keyLocation = e.getKeyLocation();
            rawCode = readPrivateLongField(e, "rawCode");
            primaryLevelUnicode = readPrivateLongField(e, "primaryLevelUnicode");
            scancode = readPrivateLongField(e, "scancode");
            extendedKeyCode = readPrivateLongField(e, "extendedKeyCode");
        }

        @Override
        public String toString() {
            return "{" +
                    "id=" + ID_TO_TYPE.getOrDefault(id, "unknown") +
                    ", modifiers=" + modifiers + "(0b" + Integer.toBinaryString(modifiers) + ")" +
                    ", keyCode=" + keyCode + "(0x" + Integer.toHexString(keyCode) + ")" +
                    ", keyChar=" + keyChar + "(" + (int) keyChar + ", 0x" + Integer.toHexString(keyChar) + ")" +
                    ", keyLocation=" + KEY_LOCATION_TO_STRING.getOrDefault(keyLocation, "unknown") +
                    ", rawCode=" + rawCode + "(0x" + Long.toHexString(rawCode) + ")" +
                    ", primaryLevelUnicode=" + primaryLevelUnicode + "(0x" + Long.toHexString(primaryLevelUnicode) + ")" +
                    ", scancode=" + scancode + "(0x" + Long.toHexString(scancode) + ")" +
                    ", extendedKeyCode=" + extendedKeyCode + "(0x" + Long.toHexString(extendedKeyCode) + ")" +
                    '}';
        }
        private static long readPrivateLongField(KeyEvent e, String name) {
            Field field;
            try {
                field = KeyEvent.class.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                return Long.MAX_VALUE;
            }
            field.setAccessible(true);
            try {
                return (long) field.get(e);
            } catch (IllegalAccessException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

    public static class EventDataJS {
        String type;
        String key;
        String code;
        String location;
        String altKey;
        String ctrlKey;
        String metaKay;
        String shiftKey;

        @Override
        public String toString() {
            return "{" +
                    "type='" + type + '\'' +
                    ", key='" + key + '\'' +
                    ", code='" + code + '\'' +
                    ", location='" + location + '\'' +
                    ", altKey='" + altKey + '\'' +
                    ", ctrlKey='" + ctrlKey + '\'' +
                    ", metaKay='" + metaKay + '\'' +
                    ", shiftKey='" + shiftKey + '\'' +
                    '}';
        }
    }
}
