#include<SoftwareSerial.h>

// Pin 3 is RX, pin 2 is TX (Arduino side)
SoftwareSerial esp8266(3, 2);

void setup() {
  Serial.begin(9600);
  esp8266.begin(115200);
  while(!Serial) {
    
  }
  
  // Set baud ESP8266 baud rate to 9600
  sendCommand("AT+CIOBAUD=9600");
  esp8266.begin(9600);
  
  // Set up ESP8266 as server
  sendCommand("AT+CIPMUX=1");
  sendCommand("AT+CIPSERVER=1,8888");
}

void loop() {
  // Listen for messages from ESP8266
  if (esp8266.available()) {
    // If we received data, first character will be a digit,
    // which belongs to the connection ID
    if (esp8266.peek() >= 48 && esp8266.peek() <= 57) {
      // Received data; process the incoming data
      processData();
    }
  }
}

void processData() {
  Serial.println("Data received");
  esp8266.find("+IPD,");
  int connectionId = esp8266.read() + 1;
  esp8266.readStringUntil(58);
  String message = esp8266.readStringUntil(0);
  message.trim();
  Serial.println(message);
  
  // If the message received is "GPS", return location
  if (message.equals("GPS")) {
    Serial.println("Location requested");
    String location = "23330 El Toro Rd, Lake Forest, CA";
    sendData(connectionId, location);
  }
  else {
    sendData(connectionId, "Hello");
  }
}

// A helper function to send a command to ESP8266
// and print the response to console
void sendCommand(String command) {
  esp8266.println(command);
  Serial.println(esp8266.readStringUntil(0));
}

// A helper function to send data to an open connection
// and print the response to console
void sendData(int connectionId, String data) {
  int responseLength = data.length() + 2;
  String sendCommand = "AT+CIPSEND=";
  sendCommand += connectionId;
  sendCommand += ",";
  sendCommand += responseLength;
  esp8266.println(sendCommand);
  Serial.println(esp8266.readStringUntil(0));
  delay(500);
  esp8266.println(data);
  Serial.println(esp8266.readStringUntil(0));
}
