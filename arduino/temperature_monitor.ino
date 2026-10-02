// The Wokwi simulation uses DHT22. For the real lab board with a DHT11,
// change the sensor type in the DHT constructor line to DHT11.

#include <DHT.h>

const int DHT_PIN = 2;
const int LED_PIN = 8;
DHT dht(DHT_PIN, DHT22);

float limitTemp = 30;
int ledMode = 2;            // 0 = OFF, 1 = ON, 2 = AUTO
int alarmState = -1;
float lastTemp = NAN;
unsigned long lastRead = 0;
String line = "";

void setup() {
  Serial.begin(9600);
  pinMode(LED_PIN, OUTPUT);
  dht.begin();
  Serial.println("STATUS:READY");
}

void loop() {
  readSerial();
  if (millis() - lastRead >= 2000) {
    lastRead = millis();
    sendReadings();
  }
}

void readSerial() {
  while (Serial.available()) {
    char c = Serial.read();
    if (c == '\n') {
      handleCommand(line);
      line = "";
    } else if (c != '\r') {
      line += c;
    }
  }
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.startsWith("LIMIT:") && cmd.length() > 6) {
    limitTemp = cmd.substring(6).toFloat();
    Serial.println("ACK:" + cmd);
  } else if (cmd == "LED:ON") {
    ledMode = 1;
    Serial.println("ACK:" + cmd);
  } else if (cmd == "LED:OFF") {
    ledMode = 0;
    Serial.println("ACK:" + cmd);
  } else if (cmd == "LED:AUTO") {
    ledMode = 2;
    Serial.println("ACK:" + cmd);
  } else {
    Serial.println("ERR:BAD_COMMAND");
    return;
  }
  checkAlarm();
}

void sendReadings() {
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  if (isnan(t) || isnan(h)) {
    Serial.println("ERR:SENSOR");
    return;
  }
  lastTemp = t;
  Serial.println("TEMP:" + String(t, 1));
  Serial.println("HUM:" + String(h, 0));
  checkAlarm();
}

void checkAlarm() {
  if (!isnan(lastTemp)) {
    int newState = (lastTemp > limitTemp) ? 1 : 0;
    if (newState != alarmState) {
      alarmState = newState;
      Serial.println("ALARM:" + String(alarmState));
    }
  }
  if (ledMode == 1) digitalWrite(LED_PIN, HIGH);
  else if (ledMode == 0) digitalWrite(LED_PIN, LOW);
  else digitalWrite(LED_PIN, alarmState == 1 ? HIGH : LOW);
}
