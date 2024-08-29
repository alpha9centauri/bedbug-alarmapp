#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include "config.h"

WiFiClient wifiClient;                // Initiate WiFi library
PubSubClient client(wifiClient);      // Initiate PubSubClient library

const unsigned char Active_buzzer = 14;

boolean enabled;
boolean online;
boolean sensorOnline;
int limitSecs;
int mobileCounter;
int counter;
float reading;
int stuckCounter;
boolean stuck;
int sensorOnlineCounter;
float oldread;
float newread;
int stuckTryCounter;


void setup() {
  pinMode (Active_buzzer,OUTPUT) ;
  enabled = false;
  online = true;
  sensorOnline = true;
  mobileCounter = 0;
  counter = 0;
  stuckCounter = 0;
  stuck = false;
  oldread = 0;
  newread = 0;
  reading = 0;
  sensorOnlineCounter = 0;
  stuckTryCounter = 0;

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
  client.setCallback(callback);                       // Set callback address, this is used for remote tare
}

// void(* resetFunc) (void) = 0; //declare reset function @ address 0

void loop() {

    Serial.println("LOOPING");

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
        Serial.println("Stuck : " + String(stuckCounter));
      }
      else {
        stuckCounter = 0;
        stuck = false;
        stuckTryCounter = 0;
        Serial.println("Good Reading");
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
//        String s = "1";
//        Serial.println("StuckTryCounter : " + String(stuckTryCounter) + " ... Resetting");
//        client.publish(RESET_TOPIC, (char *)s.c_str());
      }
    }

    // ==============================================================================
    // Sensor Online Logic
    // This logic is created because when we start monitor sensor, suppose the weight sensor is offline already,
    // so it will give no "offline" message but the sensorOnline will be set to true, which will be wrong.
    sensorOnlineCounter++;

    if(sensorOnlineCounter > 100) {
      sensorOnline = false;
      sensorOnlineCounter = 0;
      Serial.println("Sensor offline");
    }



    if(enabled && sensorOnline && !stuck) {
      Serial.println("Monitoring started ... ");
      Serial.println("Total Seconds = " + String(limitSecs));

      int limit = limitSecs;
      counter = 0;

      while(counter <= limit) {
        delay(1000);

        if(online && sensorOnline && mobileCounter <= 1000 && reading <= 20) {
           digitalWrite(Active_buzzer,LOW);
           Serial.println(String(counter) + " : Buzzer Low : Time Left (secs) : " + String(limit - counter) + " Mobile Counter : " + String(mobileCounter));
        }
        else {
          digitalWrite(Active_buzzer,HIGH);
          Serial.println(String(counter) + " : Buzzer High : Time Left (secs) : " + String(limit - counter)+ " Mobile Counter : " + String(mobileCounter));
        }

        if (!client.connected()) {
          digitalWrite(Active_buzzer,LOW);
          reconnect();
        }


        counter++;
        mobileCounter++;
        if(!sensorOnline) {
          if(punishment(limit)) {
             client.loop();
          }
        }
        else {
          client.loop();
        }
      }
      Serial.println("Monitoring Stopped");
      enabled = false;
      counter = 0;
      limitSecs = 0;
      mobileCounter = 0;
      digitalWrite(Active_buzzer, LOW);
    }

    client.loop();
    delay (3000);
}

void reconnect() {
  while (!client.connected()) {       // Loop until connected to MQTT server
    Serial.print("Attempting MQTT connection...");
    if (client.connect(HOSTNAME)) {       //Connect to MQTT server
      Serial.println("connected");
      client.setCallback(callback);
      client.subscribe(ENABLE_TOPIC, 0);       
      client.subscribe(ONLINE_TOPIC, 0);
      client.subscribe(STATE_TOPIC, 0);
      String str = "Online";
      client.publish(MONITOR_TOPIC, (char *)str.c_str());
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

boolean punishment(int limit) {
  Serial.println("Punishment");
  client.loop();
  while(counter <= limit && !sensorOnline) {
    delay(1000);
    digitalWrite(Active_buzzer,HIGH);
    Serial.println(String(counter) + " : Buzzer High : Time Left (secs) : " + String(limit - counter)+ " Mobile Counter : " +String(mobileCounter));
    if (!client.connected()) {
       digitalWrite(Active_buzzer,LOW);
       reconnect();
    }
    mobileCounter++;
    counter++;
    client.loop();
  }

  digitalWrite(Active_buzzer,LOW);
  if(counter > limit)
    return false;

  delay(3000);
  counter += 3;

  beepbeep(1);

  Serial.println("Punishment Time");
  int changeCounter = 1;
  float oldmsg = reading;
  float newmsg = reading;
  int delayCounter = 0;
  while(changeCounter <= 5 && sensorOnline) {
    delay(1000);
    if(delayCounter > 60) {
      Serial.println("Get up");
      digitalWrite(Active_buzzer,HIGH);
      Serial.println(String(counter) + " : Buzzer High : Time Left (secs) : " + String(limit - counter)+ " Mobile Counter : " + String(mobileCounter));
    }
    else{
      digitalWrite(Active_buzzer,LOW);
      Serial.println(String(counter) + " : Buzzer Low : Time Left (secs) : " + String(limit - counter) + " Mobile Counter : " + String(mobileCounter));
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
      beepbeep(2);
      digitalWrite(Active_buzzer,LOW);
      Serial.println("Change Noticed");
    }

    client.loop();
    counter++;
    delayCounter++;
  }

  Serial.println("Out of punishment");
  if(changeCounter > 5) {
    return true;
  }
  else {
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

void callback(char* topic, byte* payload, unsigned int length) {
  String str = "";
  char intarr[length];
  for(int i = 0; i < length; i++) {
    intarr[i] = (char) payload[i];
  }

  for(int i = 0; i < length; i++) {
    str += String(intarr[i]);
  }

  Serial.println("Message :" + str);

  if (strcmp(topic, ENABLE_TOPIC) == 0) {
    enabled = true;
    limitSecs = str.toInt();
    Serial.println("ENABLED");

  }
  else if(strcmp(topic, STATE_TOPIC) == 0) {

    if(str.compareTo("offline") == 0 || str.compareTo("started") == 0) {
      sensorOnline = false;
    }
    else {
      reading = str.toFloat();
      sensorOnline = true;

      if(!enabled)
        sensorOnlineCounter = 0;
    }
  }

  else if(strcmp(topic, ONLINE_TOPIC) == 0) {
    if(str.compareTo("1") == 0) {
      online = true;
      mobileCounter = 0;
    }
    else {
      online = false;
    }

  }

}
