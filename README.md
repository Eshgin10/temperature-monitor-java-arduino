# Temperature Monitor — Java + Arduino

Java Swing GUI + Arduino via USB Serial

A Java Swing desktop app displays temperature and humidity in real time, lets the user set a temperature limit, and controls an LED with ON, OFF, or AUTO mode; AUTO turns the LED on above the limit.

**Author:** Eshgin Shikhmammadov  
**Course:** Course projects 2: Java (RNU)

## Components

Arduino UNO R3, DHT11 sensor (DHT22 in the Wokwi simulation), LED + resistor (or LED module), breadboard, jumper wires, and USB cable.

## Wiring

- DHT data → Uno pin 2; VCC → 5V; GND → GND.
- LED anode → resistor → pin 8; LED cathode → GND.

## Repository structure

```text
temperature-monitor-java-arduino/
├── arduino/temperature_monitor.ino
├── java-app/
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/lv/rnu/tempmonitor/
│       │   ├── Main.java
│       │   ├── MainWindow.java
│       │   ├── SerialConnection.java
│       │   └── Protocol.java
│       └── test/java/lv/rnu/tempmonitor/ProtocolTest.java
├── docs/index.html
├── assets/.gitkeep
├── .gitignore
└── README.md
```

## How to run

1. Upload the sketch in `/arduino` using the Arduino IDE. Install the **DHT sensor library** by Adafruit. For the real DHT11 board, change `DHT22` to `DHT11` in the DHT constructor.
2. Connect the Arduino by USB.
3. Close the Arduino Serial Monitor. Build and run the Java app (Java 17 + Maven):
   ```bash
   cd java-app
   mvn package
   java -jar target/temperature-monitor.jar
   ```
   Pick the Arduino's COM port, press **Connect**, and wait for `STATUS:READY`. More details and troubleshooting: [`java-app/README.md`](java-app/README.md).

## Serial protocol

**9600 baud**, one message per line.

- Arduino → Java: `STATUS:READY`, `TEMP:<value>`, `HUM:<value>`, `ALARM:<0|1>`, `ACK:<command>`, `ERR:<text>`.
- Java → Arduino: `LIMIT:<value>`, `LED:ON`, `LED:OFF`, `LED:AUTO`.

Example conversation (arrows indicate direction and are not transmitted):

```text
Arduino → Java: STATUS:READY
Java → Arduino: LIMIT:28
Arduino → Java: ACK:LIMIT:28
Java → Arduino: LED:AUTO
Arduino → Java: ACK:LED:AUTO
Arduino → Java: TEMP:29.2
Arduino → Java: HUM:45
Arduino → Java: ALARM:1
```

## Documentation

Project report: [eshgin10.github.io/temperature-monitor-java-arduino](https://eshgin10.github.io/temperature-monitor-java-arduino/) (GitHub Pages).

Demo video of the real prototype: [`docs/media/demo.mp4`](docs/media/demo.mp4) (also embedded in the report).
