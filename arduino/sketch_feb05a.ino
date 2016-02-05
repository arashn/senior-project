#include <Adafruit_GPS.h>
#include <SoftwareSerial.h>

Adafruit_GPS GPS(&Serial2);

void setup() {
  Serial.begin(115200);
  Serial1.begin(115200);
  while(!Serial) {
    
  }
  
  // Set baud ESP8266 baud rate to 9600
  sendCommand("AT+CIOBAUD=9600");
  Serial1.begin(9600);
  
  // Set up ESP8266 as server
  sendCommand("AT+CIPMUX=1");
  sendCommand("AT+CIPSERVER=1,8888");
  sendCommand("AT+CIFSR");
  
  GPS.begin(9600);
  
  // uncomment this line to turn on RMC (recommended minimum) and GGA (fix data) including altitude
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
  // uncomment this line to turn on only the "minimum recommended" data
  //GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCONLY);
  // For parsing data, we don't suggest using anything but either RMC only or RMC+GGA since
  // the parser doesn't care about other sentences at this time
  
  // Set the update rate
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);   // 1 Hz update rate
  // For the parsing code to work nicely and have time to sort thru the data, and
  // print it out we don't suggest using anything higher than 1 Hz

  // Request updates on antenna status, comment out to keep quiet
  GPS.sendCommand(PGCMD_ANTENNA);

  delay(1000);
  // Ask for firmware version
}

void loop() {
  char c = GPS.read();
  if (c)
  Serial.print(c);
  // Listen for messages from ESP8266
  if (Serial1.available()) {
    // If we received data, first character will be a digit,
    // which belongs to the connection ID
    if (Serial1.peek() >= 48 && Serial1.peek() <= 57) {
      // Received data; process the incoming data
      processData();
    }
  }
}

void processData() {
  Serial.println("Data received");
  Serial1.find("+IPD,");
  int connectionId = Serial1.read() + 1;
  Serial1.readStringUntil(58);
  String message = Serial1.readStringUntil(0);
  message.trim();
  Serial.println(message);
  
  // If the message received is "GPS", return location
  if (message.equals("GPS")) {
    Serial.println("Location requested");
    String location = getLocation();
    sendData(connectionId, location);
  }
  else {
    sendData(connectionId, "Hello");
  }
}

// A helper function to send a command to ESP8266
// and print the response to console
void sendCommand(String command) {
  Serial1.println(command);
  Serial.println(Serial1.readStringUntil(0));
}

// A helper function to send data to an open connection
// and print the response to console
void sendData(int connectionId, String data) {
  int responseLength = data.length() + 2;
  String sendCommand = "AT+CIPSEND=";
  sendCommand += connectionId;
  sendCommand += ",";
  sendCommand += responseLength;
  Serial1.println(sendCommand);
  Serial.println(Serial1.readStringUntil(0));
  delay(500);
  Serial1.println(data);
  Serial.println(Serial1.readStringUntil(0));
}

String getLocation() {
  String location = "";
  
  if (GPS.newNMEAreceived()) {
    // a tricky thing here is if we print the NMEA sentence, or data
    // we end up not listening and catching other sentences! 
    // so be very wary if using OUTPUT_ALLDATA and trytng to print out data
    //Serial.println(GPS.lastNMEA());   // this also sets the newNMEAreceived() flag to false
  
    if (!GPS.parse(GPS.lastNMEA()))   // this also sets the newNMEAreceived() flag to false
      return location;  // we can fail to parse a sentence in which case we should just wait for another
  }
    
  if (GPS.fix) {
    location += String(GPS.latitudeDegrees, 4);
    location += ", ";
    location += String(GPS.longitudeDegrees, 4);
    Serial.print("Location (in degrees, works with Google Maps): ");
    Serial.println(location);
  }
  return location;
}
