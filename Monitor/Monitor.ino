#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include "config.h"


WiFiClient wifiClient;                // Initiate WiFi library
PubSubClient client(wifiClient);      // Initiate PubSubClient library

const unsigned char Active_buzzer = 14;

boolean enabled;
int limitSecs;

boolean sensorOnline;
// int sensorOnlineCounter;

int targetWeight;

int stuckCounter;
boolean stuck;
float oldread;
float newread;
float reading;
int stuckTryCounter;

boolean exerciseEnabled;
int waitTime;
int monitoringTime;

int counter;

void setup() {
  pinMode (Active_buzzer,OUTPUT) ;

  enabled = false;

  sensorOnline = false;
//  sensorOnlineCounter = 0;

  stuckCounter = 0;
  stuck = false;
  stuckTryCounter = 0;
  oldread = 0;
  newread = 0;


  targetWeight = 20;
  waitTime = 0;
  exerciseEnabled = false;

  counter = 0;

  Serial.begin(74880);
  Serial.println();
  WiFi.mode(WIFI_STA);
  WiFi.begin(SSID, PASSWORD);
  Serial.print("Connecting...");

  while (WiFi.status() != WL_CONNECTED) {       // Wait till Wifi connected
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  Serial.print("Connected, IP address: ");
  Serial.println(WiFi.localIP());                     // Print IP address

  client.setServer(MQTT_SERVER, 1883);                // Set MQTT server and port number
  client.setCallback(callback);
}

void loop() {
  Serial.println("LOOPING");
  printLog("LOOPING");
  printStatus("Alarm Inactive");

  if (!client.connected()) {
    reconnect();
  }

  // ================================================================================
  // Stuck Logic

  oldread = newread;
  newread = reading;

  if(sensorOnline) {
    if(oldread == newread) {
      stuckCounter++;
      Serial.println("StuckCounter : " + String(stuckCounter) + " stuck status : " + String(stuck));
      printLog("StuckCounter : " + String(stuckCounter)  + " stuck status : " + String(stuck));
    }
    else {
      stuckCounter = 0;
      stuck = false;
      stuckTryCounter = 0;
      Serial.println("Good Reading " + String(newread));
      printLog("Good Reading " + String(newread));
    }
  }else {
    stuckCounter = 0;
    stuckTryCounter = 0;
    stuck = false;
  }


  if(stuckCounter > 2400) {
    stuck = true;
    stuckCounter = 0;
    if(stuckTryCounter < 1) {
      stuckTryCounter++;
      beepbeep(2);
    }
  }

  // ==============================================================================
  // Sensor Online Logic
  // This logic is created because when we start monitor sensor, suppose the weight sensor is offline already,
  // so it will give no "offline" message but the sensorOnline will be set to true, which will be wrong.
//  sensorOnlineCounter++;

//  if(sensorOnlineCounter > 100) {
//    sensorOnline = false;
//    sensorOnlineCounter = 0;
//    Serial.println("Sensor offline");
//    printLog("Sensor offline");
//  }


  if(enabled && sensorOnline && !stuck) {

    Serial.println("ENABLED");
    printLog("Enabled");
    printStatus("Enabled");

    // Waiting Period
    
    while(waitTime > 0) {
      Serial.println("Waiting : "+ String(waitTime));
      printLog("Waiting : "+ String(waitTime));
      printStatus("Waiting : " + String(waitTime));
      delay(1000);
      waitTime--;
    }
    
    delay(waitTime * 1000);

    // Monitoring Period
    Serial.println("Monitoring Period");
    printLog("Monitoring Period");
    printStatus("Monitoring Period");
    counter = 0;
    int weight = targetWeight;
    while(counter <= monitoringTime) {
      delay(1000);
      if(sensorOnline && reading <= weight) {
        digitalWrite(Active_buzzer,LOW);
        Serial.println(String(counter) + " : Buzzer Low : Time Left (secs) : " + String(monitoringTime - counter));
        printLog(String(counter) + " : Buzzer Low : Time Left (secs) : " + String(monitoringTime - counter));
        printStatus("Time Left : " + String(monitoringTime - counter) + "s");
      }
      else {
        digitalWrite(Active_buzzer,HIGH);
        Serial.println(String(counter) + " : Buzzer High : Time Left (secs) : " + String(monitoringTime - counter));
        printLog(String(counter) + " : Buzzer High : Time Left (secs) : " + String(monitoringTime - counter));
        printStatus("Time Left : " + String(monitoringTime - counter) + "s");
      }

      if (!client.connected()) {
        digitalWrite(Active_buzzer,LOW);
        reconnect();
      }

      if(!sensorOnline) {
        Serial.println("Sensor went offline while enabled");
        printLog("Sensor went offline while enabled");
        printStatus("Sensor Offline");
        while(counter <= monitoringTime && !sensorOnline) {
          delay(1000);
          digitalWrite(Active_buzzer,HIGH);
          if (!client.connected()) {
             digitalWrite(Active_buzzer,LOW);
             reconnect();
          }
          printStatus("Bring sensor online");
          counter++;
          client.loop();
        }

        if(exercise(7))
          exerciseEnabled = false;
      }

      // First Time Exercise
      if(exerciseEnabled) {
        if(exercise(4))
          exerciseEnabled = false;
      }

      counter++;
      client.loop();
    }

    enabled = false;
    waitTime = 0;
    monitoringTime = 0;
    exerciseEnabled = false;
    counter = 0;
    digitalWrite(Active_buzzer, LOW);
    Serial.println("disabled");
    printLog("disabled");
    printStatus("Disabled");
  }
  

  client.loop();
  delay (3000);
}


boolean exercise(int numberOfSitups) {
  Serial.println("Exercise");
  printLog("Exercise");
  printStatus("Exercise");
  fastBeeps(1);
  int changeCounter = 1;
  float oldmsg = reading;
  float newmsg = reading;
  int delayCounter = 0;
  while(changeCounter <= numberOfSitups && sensorOnline) {
    delay(1000);
    
    if(delayCounter > 60) {
      Serial.println("Get up");
      printStatus("Exercise (D:" + String(60 - delayCounter) + "s|C:" + String(numberOfSitups - changeCounter + 1) + ") GET UP");
      digitalWrite(Active_buzzer,HIGH);
    }
    else {
      digitalWrite(Active_buzzer,LOW);
      printStatus("Exercise (D:" + String(60 - delayCounter) + "s|C:" + String(numberOfSitups - changeCounter + 1) + ")");
    }

    if (!client.connected()) {
       digitalWrite(Active_buzzer,LOW);
       reconnect();
    }

    oldmsg = newmsg;
    newmsg = reading;
    if(abs(newmsg - oldmsg) > 10) {
      delayCounter = 0;
      changeCounter++;
      beepbeep(1);
      digitalWrite(Active_buzzer,LOW);
      Serial.println("Change Noticed");
      printStatus("Change Noticed : Left " + String(numberOfSitups - changeCounter + 1));
    }

    client.loop();
    delayCounter++;
  }
  Serial.println("Out of exercise");
  printStatus("Out of exercise");
  if(changeCounter > numberOfSitups) {
    printStatus("Exercise Successful");
    return true;
  }
  else {
    printStatus("Exercise Unsuccessful");
    return false;
  }
}


void beepbeep(int n) {
  digitalWrite(Active_buzzer,LOW);
  delay(500);

  for(int i = 1; i <= n; i++) {
    digitalWrite(Active_buzzer,HIGH);
    delay(500);
    digitalWrite(Active_buzzer, LOW);
    delay(500);
    digitalWrite(Active_buzzer, HIGH);
    delay(500);
    digitalWrite(Active_buzzer, LOW);
    delay(1000);
  }
}

void fastBeeps(int n){
  digitalWrite(Active_buzzer,LOW);
  delay(500);

  for(int i = 1; i <= n; i++) {
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer,HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(100);
    digitalWrite(Active_buzzer, HIGH);
    delay(100);
    digitalWrite(Active_buzzer, LOW);
    delay(1000);
  }
}


void reconnect() {
  while (!client.connected()) {       // Loop until connected to MQTT server
    Serial.print("Attempting MQTT connection...");
    if (client.connect(HOSTNAME, NULL, NULL, MONITOR_TOPIC, 1, 0, "Monitor Offline")) {       //Connect to MQTT server
      Serial.println("connected");
      client.setCallback(callback);
      client.subscribe(ENABLE_TOPIC, 0);
      client.subscribe(STATE_TOPIC, 0);
      if(enabled)
        printStatus("Online and Enabled");
      else
        printStatus("Online and Disabled");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);  // Will attempt connection again in 5 seconds
      if(enabled)
        counter += 5;
    }
  }
}

void callback(char* topicS, byte* payload, unsigned int length) {
  
  String str = "";
  char intarr[length];
  for(int i = 0; i < length; i++) {
    intarr[i] = (char) payload[i];
  }

  for(int i = 0; i < length; i++) {
    str += String(intarr[i]);
  }

  Serial.println("Message :" + str);
  
  int topicLength = 0;
  while(topicS[topicLength] != '\0'){
    topicLength++;
  }
  
  char topic[topicLength + 1];
  for(int i = 0; i < topicLength + 1; i++) {
    topic[i] = topicS[i];
  }
  
  // Weight Sensor Online and Reading =========================================
  if(strcmp(topic, STATE_TOPIC) == 0) {

    if(str.compareTo("offline") == 0 || str.compareTo("started") == 0) {
      sensorOnline = false;
    }
    else {
      reading = str.toFloat();
      sensorOnline = true;

//      if(!enabled)
//        sensorOnlineCounter = 0;
    }
  }

  // Monitor Enable ===========================================================
  else if (strcmp(topic, ENABLE_TOPIC) == 0) {
    if(!enabled){ // So that another enable request can not be sent when already enabled

      if(!sensorOnline && !stuck) {
        Serial.println("Enable Failure : sensor offline");
        printLog("Enable Failure : sensor offline");
        printStatus("Enable Failure");
      }
      else if(stuck && sensorOnline) {
        Serial.println("Enable Failure : stuck");
        printLog("Enable Failure : stuck");
        printStatus("Enable Failure");
      }
      else if(stuck && !sensorOnline) {
        Serial.println("Enable Failure : sensorOffline and stuck");
        printLog("Enable Failure : sensorOffline and stuck");
        printStatus("Enable Failure");
      }
      else {
        enabled = true;
  
        int mesgArr[] = {0, 0, 0, 0};
        // exerciseEnabled MonitoringTime WaitTime TargetWeight
        // 1 300 20 10-> exercise is enabled, monitoring time is 300 secs, wait time is 20 secs, targetWeight is 10 kgs
        // 0 300 0 40-> exercise is disabled, monitoring time is 300 secs, wait time is 0 secs, targetWeight is 40 kgs
  
        int c = 0;
        String word = "";
        for(int i = 0; i < str.length(); i++) {
          if(str.charAt(i) == ' ') {
            mesgArr[c] = word.toInt();
            c++;
            word = "";
          }
          else {
            word = word + String(str.charAt(i));
          }
        }
        mesgArr[c] = word.toInt();
  
        if(mesgArr[0]) {
          exerciseEnabled = true;
        }
        else {
          exerciseEnabled = false;
        }
        monitoringTime = mesgArr[1];
        waitTime = mesgArr[2];
        targetWeight = mesgArr[3];
      }
    }
    else {
      printStatus("not disabled yet");
      printLog("enable request received but not disabled yet");
      Serial.println("enable request received but not disabled yet");
    }
  }
}


void printLog(String str) {
  str = "MONITOR : " + str;

  client.publish(LOG_TOPIC,  (char *)str.c_str());
}

void printStatus(String str) {
  client.publish(MONITOR_TOPIC,  (char *)str.c_str());
}
