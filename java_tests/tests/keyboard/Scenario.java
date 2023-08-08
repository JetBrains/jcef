package tests.keyboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        String result = name + "(" + eventsJava.size() + " events)";
        if (comments != null && !comments.isEmpty()) {
            result = result + "[" + comments + "]";
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scenario scenario = (Scenario) o;
        return Objects.equals(name, scenario.name) && Objects.equals(comments, scenario.comments) && Objects.equals(eventsJava, scenario.eventsJava) && Objects.equals(eventsJSExpected, scenario.eventsJSExpected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, comments, eventsJava, eventsJSExpected);
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
        public int id;
        public int modifiers;
        public int keyCode;
        public char keyChar;
        public int keyLocation;
        private final long rawCode;
        private final long primaryLevelUnicode;
        private final long scancode; // for MS Windows only
        private final long extendedKeyCode;

        EventDataJava(KeyEvent e) {
            id = e.getID();
            modifiers = e.getModifiersEx();
            keyCode = e.getKeyCode();
            keyChar = e.getKeyChar();
            keyLocation = e.getKeyLocation();
            rawCode = getPrivateLongField(e, "rawCode");
            primaryLevelUnicode = getPrivateLongField(e, "primaryLevelUnicode");
            scancode = getPrivateLongField(e, "scancode");
            extendedKeyCode = getPrivateLongField(e, "extendedKeyCode");
        }

        public KeyEvent makeKeyEvent(Component component) {
            KeyEvent result = new KeyEvent(component, id, 0, modifiers, keyCode, keyChar, keyLocation);
            setPrivateLongField(result, "rawCode", rawCode);
            setPrivateLongField(result, "primaryLevelUnicode", primaryLevelUnicode);
            setPrivateLongField(result, "scancode", scancode);
            setPrivateLongField(result, "extendedKeyCode", extendedKeyCode);
            return result;
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

        private static long getPrivateLongField(KeyEvent e, String name) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventDataJava that = (EventDataJava) o;
            return id == that.id && modifiers == that.modifiers && keyCode == that.keyCode && keyChar == that.keyChar && keyLocation == that.keyLocation && rawCode == that.rawCode && primaryLevelUnicode == that.primaryLevelUnicode && scancode == that.scancode && extendedKeyCode == that.extendedKeyCode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, modifiers, keyCode, keyChar, keyLocation, rawCode, primaryLevelUnicode, scancode, extendedKeyCode);
        }
    }

    private static void setPrivateLongField(KeyEvent e, String name, long value) {
        Field field;
        try {
            field = KeyEvent.class.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            return;
        }
        field.setAccessible(true);
        try {
            field.set(e, value);
        } catch (IllegalAccessException ignored) {
        }

    }

    public static class EventDataJS {
        public final String type;
        public final String key;
        public final String code;
        public final String location;
        public final String altKey;
        public final String ctrlKey;
        public final String metaKay;
        public final String shiftKey;

        public static EventDataJS fromJson(String jsonText) {
            return new Gson().fromJson(jsonText, EventDataJS.class);
        }

        public String toJson() {
            return new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(this);
        }


        private EventDataJS(String type, String key, String code, String location, String altKey, String ctrlKey, String metaKay, String shiftKey) {
            this.type = type;
            this.key = key;
            this.code = code;
            this.location = location;
            this.altKey = altKey;
            this.ctrlKey = ctrlKey;
            this.metaKay = metaKay;
            this.shiftKey = shiftKey;
        }

        @Override
        public String toString() {
            return toJson();
//            return "{" +
//                    "type='" + type + '\'' +
//                    ", key='" + key + '\'' +
//                    ", code='" + code + '\'' +
//                    ", location='" + location + '\'' +
//                    ", altKey='" + altKey + '\'' +
//                    ", ctrlKey='" + ctrlKey + '\'' +
//                    ", metaKay='" + metaKay + '\'' +
//                    ", shiftKey='" + shiftKey + '\'' +
//                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventDataJS that = (EventDataJS) o;
            return Objects.equals(type, that.type) && Objects.equals(key, that.key) && Objects.equals(code, that.code) && Objects.equals(location, that.location) && Objects.equals(altKey, that.altKey) && Objects.equals(ctrlKey, that.ctrlKey) && Objects.equals(metaKay, that.metaKay) && Objects.equals(shiftKey, that.shiftKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, key, code, location, altKey, ctrlKey, metaKay, shiftKey);
        }
    }
}
