package lv.rnu.tempmonitor;

import java.math.BigDecimal;

/**
 * The text protocol between the app and the Arduino.
 * Every message is one line: NAME:value
 *
 * Arduino -> Java: STATUS:READY, TEMP:29.2, HUM:45, ALARM:1, ACK:LED:AUTO, ERR:BAD_COMMAND
 * Java -> Arduino: LIMIT:28, LED:ON, LED:OFF, LED:AUTO
 */
public class Protocol {

    /** Must be the same as Serial.begin(9600) in the Arduino sketch. */
    public static final int BAUD_RATE = 9600;

    // Commands the app can send.
    public static final String LED_ON = "LED:ON";
    public static final String LED_OFF = "LED:OFF";
    public static final String LED_AUTO = "LED:AUTO";

    // The sensors' range (DHT22: -40 to 80 °C). A limit outside it makes no sense.
    public static final double MIN_LIMIT = -40;
    public static final double MAX_LIMIT = 80;

    /** One message split into its name and value, for example "TEMP" and "29.2". */
    public record Message(String name, String value) { }

    /**
     * Splits a line at the FIRST colon.
     * "TEMP:29.2"     -> name "TEMP", value "29.2"
     * "ACK:LIMIT:28"  -> name "ACK",  value "LIMIT:28"
     * "HELLO"         -> name "HELLO", value ""
     */
    public static Message parse(String line) {
        String text = line.trim();
        int colon = text.indexOf(':');
        if (colon < 0) {
            return new Message(text, "");
        }
        return new Message(text.substring(0, colon), text.substring(colon + 1));
    }

    /**
     * Checks the text from the limit field and builds the LIMIT command.
     * "28" -> "LIMIT:28", "28.50" -> "LIMIT:28.5", "27,5" -> "LIMIT:27.5"
     *
     * @throws IllegalArgumentException if the text is not a number or out of range.
     *         The message of the exception is shown to the user.
     */
    public static String limitCommand(String text) {
        String clean = text.trim().replace(',', '.');   // allow a comma as decimal point
        BigDecimal number;
        try {
            number = new BigDecimal(clean);              // rejects "abc", "", "NaN"
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a number, for example 28 or 27.5");
        }
        double value = number.doubleValue();
        if (value < MIN_LIMIT || value > MAX_LIMIT) {
            throw new IllegalArgumentException("The limit must be between "
                    + (int) MIN_LIMIT + " and " + (int) MAX_LIMIT + " °C");
        }
        // stripTrailingZeros: 28.50 -> 28.5, toPlainString: never "2.8E+1"
        String plain = number.stripTrailingZeros().toPlainString();
        return "LIMIT:" + plain;
    }
}
