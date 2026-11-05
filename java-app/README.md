# Temperature Monitor — Java app

A Java Swing desktop app that talks to the Arduino sketch in [`../arduino`](../arduino/temperature_monitor.ino) over USB Serial.

## What it does

- Lists the COM ports on the computer (**Refresh** updates the list). **Connect** / **Disconnect** open and close the port.
- Shows the live **temperature** and **humidity** in large labels.
- Shows the **alarm**: red `ALARM` for `ALARM:1`, green `OK` for `ALARM:0`.
- **Set limit**: checks that the text is a number from -40 to 80, then sends `LIMIT:<value>`. If the text is not valid, it shows an error message.
- **LED ON / LED OFF / LED AUTO** send `LED:ON`, `LED:OFF`, `LED:AUTO`.
- Shows the limit and LED mode the Arduino confirmed (from `ACK` lines).
- A **log** shows every line with its direction: `-> LED:AUTO` (sent), `<- TEMP:29.2` (received), including `ACK` and `ERR` lines.
- **Status bar**: `Disconnected` → `Connected to COMx — waiting for Arduino ...` → `Arduino ready on COMx` (after `STATUS:READY`).
- Handles errors without crashing: port not found, port busy, cable unplugged while in use.

## Code

Package `lv.rnu.tempmonitor`, in `src/main/java/lv/rnu/tempmonitor/`:

| Class | Job |
|---|---|
| `Main` | Starts the program and opens the window on the Swing thread. |
| `MainWindow` | The Swing window: buttons, labels, log, status bar. Reacts to each message. |
| `SerialConnection` | Opens/closes the COM port with **jSerialComm**, sends lines, and reads lines in a background thread. |
| `Protocol` | Protocol constants, splits a line into name + value, checks the limit and builds `LIMIT:<value>`. |

`src/test/java/.../ProtocolTest.java` has unit tests for the parsing and the limit check.

The background reader thread never touches Swing. It passes every line to `MainWindow`, and `MainWindow` updates the labels through `SwingUtilities.invokeLater`.

## Requirements

- **Java 17** or newer (JDK to build, JRE/JDK to run)
- **Maven 3.6+** to build
- No serial driver or native library to install. jSerialComm contains them inside the JAR. (Some Arduino clones with a CH340 chip need the CH340 USB driver from the board seller.)

## Build

```bash
cd java-app
mvn package
```

This compiles the code, runs the unit tests and creates **`target/temperature-monitor.jar`**. The JAR includes jSerialComm.

## Run

```bash
java -jar target/temperature-monitor.jar
```

## Choose the COM port

1. Upload the sketch to the Arduino and plug it in by USB.
2. **Close the Arduino IDE Serial Monitor.** Only one program can use a COM port.
3. In the app, press **Refresh** and pick the Arduino's port in the list.
   - Not sure which one? In the Arduino IDE, look at *Tools → Port*. Or open the Windows *Device Manager → Ports (COM & LPT)* and see which port appears when you plug the board in.
4. Press **Connect**. Opening the port restarts the Arduino UNO. After about 2 seconds the log shows `<- STATUS:READY`, and the status bar says *Arduino ready*.
5. Values appear every 2 seconds. Now you can send a limit and choose the LED mode.

## Troubleshooting

| Problem | What to do |
|---|---|
| **"Could not open COMx. The port may be busy"** | Another program has the port open, usually the Arduino Serial Monitor or a second copy of this app. Close it and press Connect again. |
| **"Port COMx was not found"** | The board was unplugged or got a new port number. Plug it in, press **Refresh** and pick the port again. |
| **No COM ports in the list** | Check the USB cable (some cables carry power only). Install the board's USB driver if needed. |
| **"Connection lost"** | The cable was unplugged while connected. Plug it back in, press Refresh and Connect. |
| **Strange characters in the log, or no values** | Wrong baud rate. The sketch uses `Serial.begin(9600)` and the app uses 9600. Both must be the same. Also check that the right sketch is uploaded. |
| **Status stays "waiting for Arduino ..."** | `STATUS:READY` did not arrive. Press the reset button on the Arduino, or Disconnect and Connect again. Check that the sketch was uploaded. Commands sent before `STATUS:READY` can be lost while the Arduino restarts. |
| **`ERR:SENSOR` in the log** | The Arduino cannot read the DHT sensor. Check the wiring (data on pin 2) and the sensor type (`DHT11` or `DHT22`) in the sketch. |
| **`ERR:BAD_COMMAND` in the log** | The Arduino did not understand a command. The app only sends valid commands, so check the sketch version. |
