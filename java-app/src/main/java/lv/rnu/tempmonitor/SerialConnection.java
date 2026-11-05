package lv.rnu.tempmonitor;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Talks to the Arduino through a COM port, using the jSerialComm library.
 *
 * - connect(): opens the port at 9600 baud and starts a background reader thread.
 * - send():    writes one line (the command + "\n").
 * - The reader thread collects bytes into lines and gives each line to the Listener.
 *
 * IMPORTANT: the Listener methods are called on the background thread,
 * NOT on the Swing thread. The window must use SwingUtilities.invokeLater.
 */
public class SerialConnection {

    /** Who wants to get the lines from the Arduino (here: MainWindow). */
    public interface Listener {
        /** A full line arrived, without the "\r\n" at the end. */
        void onLineReceived(String line);

        /** The connection broke by itself, for example the USB cable was unplugged. */
        void onConnectionLost();
    }

    private final Listener listener;
    private volatile SerialPort port;   // volatile: used by more than one thread
    private Thread readerThread;

    // "volatile" because two threads read this: the Swing thread and the reader thread.
    private volatile boolean running = false;

    public SerialConnection(Listener listener) {
        this.listener = listener;
    }

    /** Returns the names of all COM ports on this computer, for example "COM3". */
    public static String[] listPortNames() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }

    /** Opens the port. Throws IOException with a clear message if it does not work. */
    public void connect(String portName) throws IOException {
        // 1. Does the port exist at all?
        boolean found = false;
        for (String name : listPortNames()) {
            if (name.equals(portName)) {
                found = true;
            }
        }
        if (!found) {
            throw new IOException("Port " + portName + " was not found. Is the Arduino plugged in? Press Refresh.");
        }

        // 2. Set 9600 baud, 8 data bits, 1 stop bit, no parity (the Arduino default "8N1").
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(Protocol.BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

        // 3. Open it. This fails if another program (e.g. the Arduino Serial Monitor) uses the port.
        if (!port.openPort()) {
            port = null;
            throw new IOException("Could not open " + portName
                    + ". The port may be busy: close the Arduino Serial Monitor and try again.");
        }

        // 4. Start reading in the background, so the window never freezes.
        running = true;
        readerThread = new Thread(this::readLoop, "serial-reader");
        readerThread.setDaemon(true);   // do not keep the program alive when the window closes
        readerThread.start();
    }

    /** Closes the port. Safe to call more than once. */
    public void disconnect() {
        running = false;                // tell the reader thread to stop first
        if (port != null) {
            port.closePort();
            port = null;
        }
    }

    public boolean isConnected() {
        return running && port != null;
    }

    /** Sends one command, for example "LED:ON". A newline is added at the end. */
    public void send(String command) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected.");
        }
        byte[] data = (command + "\n").getBytes(StandardCharsets.US_ASCII);
        int written = port.writeBytes(data, data.length);
        if (written != data.length) {
            throw new IOException("Could not send \"" + command + "\". Is the cable still connected?");
        }
    }

    /**
     * Runs on the background thread.
     * Reads bytes, builds lines, and calls the listener for every full line.
     */
    private void readLoop() {
        SerialPort myPort = port;
        StringBuilder line = new StringBuilder();
        byte[] buffer = new byte[256];

        while (running) {
            int available = myPort.bytesAvailable();

            if (available < 0) {
                // -1 means the port is gone (cable unplugged) or was closed.
                if (running) {          // only report it if WE did not close it
                    running = false;
                    myPort.closePort();
                    listener.onConnectionLost();
                }
                return;
            }

            if (available == 0) {
                sleep(20);              // nothing new yet, wait a little
                continue;
            }

            int count = myPort.readBytes(buffer, Math.min(available, buffer.length));
            for (int i = 0; i < count; i++) {
                char c = (char) buffer[i];
                if (c == '\n') {                    // end of a message
                    if (line.length() > 0) {
                        listener.onLineReceived(line.toString());
                    }
                    line.setLength(0);
                } else if (c != '\r') {             // ignore '\r' (Arduino println sends "\r\n")
                    line.append(c);
                }
            }
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
