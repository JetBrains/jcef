package tests.junittests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TestSetupExtension.class)
class StringTest {
    @Test
    void testJava2NativeStringConversion() {
        try {
            System.loadLibrary("jceftesthelpers");
        } catch (Throwable e) {
            System.out.println("Can't load native library 'jceftesthelpers', " +
                    "test will be skipped (jvm isn't intended for this test)");
            return;
        }

        String s = null;
        String s2 = convertString(s);
        assertTrue(s2 == null || s2.isEmpty());

        s = "Ascii string";
        assertEquals(s, convertString(s));

        s = "Юникод";
        assertEquals(s, convertString(s));

        s = "Cookie symbol: 🍪.";
        assertEquals(s, convertString(s));

        StringBuilder sb = new StringBuilder("Very-very long string, ");
        for (int c = 0; c < 100000; ++c)
            sb.append('a');
        sb.append('.');
        s = sb.toString();
        assertEquals(s, convertString(s));

        s = "Chinese string: 三等奖付款就上课地方";
        assertEquals(s, convertString(s));
    }

    static native String convertString(String s);
}
