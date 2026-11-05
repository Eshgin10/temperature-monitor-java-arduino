package lv.rnu.tempmonitor;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The Swing window.
 *
 * Layout (top to bottom):
 *   1. Connection row: COM port list, Refresh, Connect, Disconnect
 *   2. Live values:    temperature, humidity, alarm
 *   3. Controls:       limit field + Set limit, LED ON / OFF / AUTO
 *   4. Log:            every line sent (->) and received (<-)
 *   5. Status bar:     Disconnected / Connected to COMx / Arduino ready
 *
 * It implements SerialConnection.Listener, so it gets every line from the Arduino.
 */
public class MainWindow extends JFrame implements SerialConnection.Listener {

    private static final Color RED = new Color(200, 30, 30);
    private static final Color GREEN = new Color(20, 140, 60);
    private static final Color GREY = new Color(120, 120, 120);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SerialConnection serial = new SerialConnection(this);
    private String connectedPort = null;     // e.g. "COM3" while connected

    // What the Arduino is using now. Updated from STATUS:READY and ACK lines.
    private String arduinoLimit = "?";
    private String arduinoLedMode = "?";

    // 1. Connection row
    private final JComboBox<String> portBox = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");

    // 2. Live values
    private final JLabel tempLabel = bigLabel("-- °C");
    private final JLabel humLabel = bigLabel("-- %");
    private final JLabel alarmLabel = bigLabel("--");
    private final JLabel arduinoStateLabel = new JLabel();

    // 3. Controls
    private final JTextField limitField = new JTextField("28", 5);
    private final JButton setLimitButton = new JButton("Set limit");
    private final JButton ledOnButton = new JButton("LED ON");
    private final JButton ledOffButton = new JButton("LED OFF");
    private final JButton ledAutoButton = new JButton("LED AUTO");

    // 4. Log and 5. status bar
    private final JTextArea logArea = new JTextArea(10, 50);
    private final JLabel statusBar = new JLabel();

    public MainWindow() {
        super("Temperature Monitor — Java + Arduino");
        buildLayout();
        connectActions();

        // Close the port when the window closes.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                serial.disconnect();
            }
        });

        refreshPorts();
        showDisconnected("Disconnected");
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);   // center on screen
    }

    // ------------------------------------------------------------------
    // Building the window
    // ------------------------------------------------------------------

    private void buildLayout() {
        // 1. Connection row
        JPanel connectionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionRow.add(new JLabel("COM port:"));
        portBox.setPrototypeDisplayValue("COM100   ");
        connectionRow.add(portBox);
        connectionRow.add(refreshButton);
        connectionRow.add(connectButton);
        connectionRow.add(disconnectButton);

        // 2. Live values: three boxes side by side
        JPanel values = new JPanel(new GridLayout(1, 3, 10, 0));
        values.add(valueBox("Temperature", tempLabel));
        values.add(valueBox("Humidity", humLabel));
        values.add(valueBox("Alarm", alarmLabel));
        alarmLabel.setOpaque(true);
        alarmLabel.setForeground(Color.WHITE);
        alarmLabel.setBackground(GREY);

        JPanel stateRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stateRow.add(arduinoStateLabel);

        // 3. Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Temperature limit (°C):"));
        controls.add(limitField);
        controls.add(setLimitButton);
        controls.add(new JLabel("     LED:"));
        controls.add(ledOnButton);
        controls.add(ledOffButton);
        controls.add(ledAutoButton);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(connectionRow);
        values.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        top.add(values);
        top.add(stateRow);
        top.add(controls);

        // 4. Log
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log  (-> sent to Arduino,  <- received from Arduino)"));

        // 5. Status bar
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel root = new JPanel(new BorderLayout(0, 4));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        root.add(top, BorderLayout.NORTH);
        root.add(logScroll, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** A box with a small title and a big value under it. */
    private static JPanel valueBox(String title, JLabel valueLabel) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createTitledBorder(title));
        box.add(valueLabel, BorderLayout.CENTER);
        return box;
    }

    private static JLabel bigLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        label.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return label;
    }

    /** Tells each button what to do when it is clicked. */
    private void connectActions() {
        refreshButton.addActionListener(e -> refreshPorts());
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());

        setLimitButton.addActionListener(e -> sendLimit());
        limitField.addActionListener(e -> sendLimit());     // pressing Enter also works

        ledOnButton.addActionListener(e -> sendCommand(Protocol.LED_ON));
        ledOffButton.addActionListener(e -> sendCommand(Protocol.LED_OFF));
        ledAutoButton.addActionListener(e -> sendCommand(Protocol.LED_AUTO));
    }

    // ------------------------------------------------------------------
    // Connection
    // ------------------------------------------------------------------

    private void refreshPorts() {
        portBox.removeAllItems();
        for (String name : SerialConnection.listPortNames()) {
            portBox.addItem(name);
        }
        if (portBox.getItemCount() == 0) {
            log("-- No COM ports found. Plug in the Arduino and press Refresh.");
        }
    }

    private void connect() {
        String portName = (String) portBox.getSelectedItem();
        if (portName == null) {
            showError("No COM port selected. Plug in the Arduino and press Refresh.");
            return;
        }

        // Disable the buttons while we try, so the user cannot click twice.
        connectButton.setEnabled(false);
        refreshButton.setEnabled(false);
        portBox.setEnabled(false);
        statusBar.setText("Connecting to " + portName + " ...");

        // Opening a port can take a moment, so do it on a background thread.
        new Thread(() -> {
            try {
                serial.connect(portName);
                SwingUtilities.invokeLater(() -> showConnected(portName));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    showDisconnected("Disconnected");
                    log("-- " + ex.getMessage());
                    showError(ex.getMessage());
                });
            }
        }, "serial-connect").start();
    }

    private void disconnect() {
        serial.disconnect();
        log("-- Disconnected from " + connectedPort);
        showDisconnected("Disconnected");
    }

    /** Called on the Swing thread after the port opened. */
    private void showConnected(String portName) {
        connectedPort = portName;
        log("-- Connected to " + portName + " at " + Protocol.BAUD_RATE + " baud");
        // Opening the port restarts the Arduino UNO, so we wait for STATUS:READY.
        statusBar.setText("Connected to " + portName + " — waiting for Arduino ...");
        statusBar.setForeground(Color.BLACK);
        setControlsEnabled(true);
    }

    private void showDisconnected(String status) {
        connectedPort = null;
        statusBar.setText(status);
        statusBar.setForeground(GREY);
        setControlsEnabled(false);
        tempLabel.setText("-- °C");
        humLabel.setText("-- %");
        setAlarm(null);
        arduinoStateLabel.setText(" ");
    }

    /** Connected: command buttons on. Disconnected: only the connection buttons on. */
    private void setControlsEnabled(boolean connected) {
        portBox.setEnabled(!connected);
        refreshButton.setEnabled(!connected);
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        limitField.setEnabled(connected);
        setLimitButton.setEnabled(connected);
        ledOnButton.setEnabled(connected);
        ledOffButton.setEnabled(connected);
        ledAutoButton.setEnabled(connected);
    }

    // ------------------------------------------------------------------
    // Sending commands (Java -> Arduino)
    // ------------------------------------------------------------------

    private void sendLimit() {
        String command;
        try {
            command = Protocol.limitCommand(limitField.getText());   // checks it is a number
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            limitField.requestFocus();
            return;
        }
        sendCommand(command);
    }

    private void sendCommand(String command) {
        try {
            serial.send(command);
            log("-> " + command);
        } catch (IOException ex) {
            log("-- " + ex.getMessage());
            showError(ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Receiving lines (Arduino -> Java)
    // These two methods are called by the BACKGROUND reader thread.
    // We must not touch Swing there, so we pass the work to the Swing thread.
    // ------------------------------------------------------------------

    @Override
    public void onLineReceived(String line) {
        SwingUtilities.invokeLater(() -> handleLine(line));
    }

    @Override
    public void onConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            String port = connectedPort;
            log("-- Connection to " + port + " lost");
            showDisconnected("Disconnected (connection lost)");
            showError("The connection to " + port + " was lost.\nCheck the USB cable, press Refresh and connect again.");
        });
    }

    /** Runs on the Swing thread. Shows one received line in the window. */
    private void handleLine(String line) {
        log("<- " + line);
        Protocol.Message msg = Protocol.parse(line);

        switch (msg.name()) {
            case "STATUS":
                if (msg.value().equals("READY")) {
                    statusBar.setText("Arduino ready on " + connectedPort);
                    statusBar.setForeground(GREEN);
                    // After a restart the Arduino uses its default values (see the .ino file).
                    arduinoLimit = "30 °C (default)";
                    arduinoLedMode = "AUTO (default)";
                    updateArduinoStateLabel();
                }
                break;
            case "TEMP":
                tempLabel.setText(msg.value() + " °C");
                break;
            case "HUM":
                humLabel.setText(msg.value() + " %");
                break;
            case "ALARM":
                setAlarm(msg.value());
                break;
            case "ACK":
                showAck(msg.value());
                break;
            case "ERR":
                if (msg.value().equals("SENSOR")) {
                    tempLabel.setText("-- °C");    // the reading failed, do not show an old value
                    humLabel.setText("-- %");
                }
                break;
            default:
                // Unknown line: it is already in the log, nothing else to do.
                break;
        }
    }

    /** ALARM:1 = red "ALARM", ALARM:0 = green "OK", nothing yet = grey "--". */
    private void setAlarm(String value) {
        if ("1".equals(value)) {
            alarmLabel.setText("ALARM");
            alarmLabel.setBackground(RED);
        } else if ("0".equals(value)) {
            alarmLabel.setText("OK");
            alarmLabel.setBackground(GREEN);
        } else {
            alarmLabel.setText("--");
            alarmLabel.setBackground(GREY);
        }
    }

    /** The Arduino confirmed a command, e.g. "LIMIT:28" or "LED:ON". Remember what it uses now. */
    private void showAck(String command) {
        if (command.startsWith("LIMIT:")) {
            arduinoLimit = command.substring("LIMIT:".length()) + " °C";
        } else if (command.startsWith("LED:")) {
            arduinoLedMode = command.substring("LED:".length());
        }
        updateArduinoStateLabel();
    }

    private void updateArduinoStateLabel() {
        arduinoStateLabel.setText("Limit on Arduino: " + arduinoLimit + "    LED mode: " + arduinoLedMode);
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private void log(String text) {
        logArea.append(LocalTime.now().format(TIME) + "  " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());   // scroll to the end
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Temperature Monitor", JOptionPane.ERROR_MESSAGE);
    }
}
