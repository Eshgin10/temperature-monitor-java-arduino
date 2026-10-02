# Temperature Monitor — Java + Arduino

Java Swing GUI + Arduino via USB Serial

A Java Swing desktop app will display temperature and humidity in real time, let the user set a temperature limit, and control an LED with ON, OFF, or AUTO mode; AUTO turns the LED on above the limit.

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
├── java-app/README.md
├── docs/index.html
├── assets/.gitkeep
├── .gitignore
└── README.md
```

## How to run

1. Upload the sketch in `/arduino` using the Arduino IDE. Install the **DHT sensor library** by Adafruit. For the real DHT11 board, change `DHT22` to `DHT11` in the DHT constructor.
2. Connect the Arduino by USB.
3. Run the Java app and select the COM port — **coming soon**.

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

[GitHub Pages report](https://eshgin10.github.io/temperature-monitor-java-arduino/) — Report coming soon.
