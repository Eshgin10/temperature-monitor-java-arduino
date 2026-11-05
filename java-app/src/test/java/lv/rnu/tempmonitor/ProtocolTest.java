package lv.rnu.tempmonitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks the message parsing and the limit check without any hardware. */
class ProtocolTest {

    @Test
    void parsesEveryArduinoMessage() {
        // These are the exact lines the Arduino sketch prints.
        assertParsed("STATUS:READY", "STATUS", "READY");
        assertParsed("TEMP:29.2", "TEMP", "29.2");
        assertParsed("HUM:45", "HUM", "45");
        assertParsed("ALARM:1", "ALARM", "1");
        assertParsed("ALARM:0", "ALARM", "0");
        assertParsed("ACK:LIMIT:28", "ACK", "LIMIT:28");   // split only at the first colon
        assertParsed("ACK:LED:AUTO", "ACK", "LED:AUTO");
        assertParsed("ERR:BAD_COMMAND", "ERR", "BAD_COMMAND");
        assertParsed("ERR:SENSOR", "ERR", "SENSOR");
    }

    @Test
    void parsesLineWithoutColonOrWithSpaces() {
        assertParsed("HELLO", "HELLO", "");
        assertParsed("  TEMP:24.0  ", "TEMP", "24.0");
    }

    @Test
    void buildsLimitCommand() {
        assertEquals("LIMIT:28", Protocol.limitCommand("28"));
        assertEquals("LIMIT:28", Protocol.limitCommand(" 28.00 "));
        assertEquals("LIMIT:27.5", Protocol.limitCommand("27,5"));
        assertEquals("LIMIT:-5", Protocol.limitCommand("-5"));
        assertEquals("LIMIT:80", Protocol.limitCommand("80"));
    }

    @Test
    void rejectsBadLimit() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.limitCommand(""));
        assertThrows(IllegalArgumentException.class, () -> Protocol.limitCommand("abc"));
        assertThrows(IllegalArgumentException.class, () -> Protocol.limitCommand("NaN"));
        assertThrows(IllegalArgumentException.class, () -> Protocol.limitCommand("28 C"));
        assertThrows(IllegalArgumentException.class, () -> Protocol.limitCommand("100"));
        assertThrows(IllegalArgumentException.class, () -> Protocol.limitCommand("-41"));
    }

    private static void assertParsed(String line, String name, String value) {
        Protocol.Message msg = Protocol.parse(line);
        assertEquals(name, msg.name(), "name of " + line);
        assertEquals(value, msg.value(), "value of " + line);
    }
}
