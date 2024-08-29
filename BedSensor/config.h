
// Wifi Settings
#define SSID                          "******"
#define PASSWORD                      "******"

// MQTT Settings
#define HOSTNAME                      "mobile-monitor"
#define MQTT_SERVER                   "broker.hivemq.com"
#define STATE_TOPIC                   "com.varunchandra.bedsensor/bedbug/kgValue"
#define STATE_RAW_TOPIC               "com.varunchandra.bedsensor/bedbug/kgValue/raw"
#define AVAILABILITY_TOPIC            "com.varunchandra.bedsensor/bedbug/available"
#define TARE_TOPIC                    "com.varunchandra.bedsensor/bedbug/tare"
#define RESET_TOPIC                   "com.varunchandra.bedsensor/bedbug/reset"

#define ENABLE_TOPIC                  "com.varunchandra.bedsensor/bedbug/monitor/enable1"
#define ONLINE_TOPIC                  "com.varunchandra.bedsensor/bedbug/monitor/online1"
#define MONITOR_TOPIC                 "com.varunchandra.bedsensor/bedbug/monitor/status"
#define TARGET_WEIGHT                 "com.varunchandra.bedsensor/bedbug/monitor/target_weight"
#define LOG_TOPIC                     "com.varunchandra.bedsensor/bedbug/log"
#define mqtt_username                 "mqtt_username"
#define mqtt_password                 "mqtt_password"

// HX711 Pins
const int LOADCELL_DOUT_PIN = 2; // Remember these are ESP GPIO pins, they are not the physical pins on the board.
const int LOADCELL_SCK_PIN = 3;
int calibration_factor = 10000; // Defines calibration factor we'll use for calibrating.
