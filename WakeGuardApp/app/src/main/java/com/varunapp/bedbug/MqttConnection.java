package com.varunapp.wakeguard;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class MqttConnection {

    public static final String TAG = "MqttConnection";

    public static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    public static final String TOPIC_KGVALUE = "com.varunchandra.bedsensor/wakeguard/kgValue";
    public static final String TOPIC_TARE = "com.varunchandra.bedsensor/wakeguard/tare";
    public static final String TOPIC_CALIBRATION_FACTOR = "com.varunchandra.bedsensor/wakeguard/caibrationFactor";
    public static final String TOPIC_MONITOR = "com.varunchandra.bedsensor/wakeguard/monitor/status";
    public static final String TOPIC_ENABLE = "com.varunchandra.bedsensor/wakeguard/monitor/enable1";
    public static final String CLIENT_ID = "varunApp";

    private String url;
    private String clientId;
    private MqttAndroidClient client;
    private Context context;
    private String topic;
    private boolean connectSuccess;
    private boolean receivingMessages;
    private String message;
    private boolean tryingToDisconnect;
    private boolean deliveryStatus;
    private MqttConnectOptions options;

    public MqttConnection(Context context, String url, String clientId, String topic) {
        this.url = url;
        this.clientId = clientId;
        this.context = context;
        this.connectSuccess = false;
        this.receivingMessages = false;
        this.topic = topic;
        this.message = null;
        this.client = new MqttAndroidClient(context, url, clientId);
        this.tryingToDisconnect = false;
        this.deliveryStatus = false;
        options = new MqttConnectOptions();
    }

    public void connectAndSubscribe() {
        try {
            options.setCleanSession(false);
            options.setAutomaticReconnect(true);
            IMqttToken token = client.connect(options);

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connection Successful " + topic);
                    connectSuccess = true;
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Connection Failure");
                    connectSuccess = false;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(!tryingToDisconnect) {
                        Log.d(TAG, "Trying to reconnect ... ");
                        connectAndSubscribe();
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribe() {
        try{
            client.subscribe(topic, 0);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Connection Lost");
                    connectSuccess = false;
                    receivingMessages = false;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(!tryingToDisconnect) {
                        Log.d(TAG, "Trying to reconnect ... ");
                        connectAndSubscribe();
                    }
                }

                @Override
                public void messageArrived(String messageTopic, MqttMessage messageArrived) throws Exception {
                    if(topic.equals(messageTopic)){
                        receivingMessages = true;

                        String newMsg = new String(messageArrived.getPayload());

                        if (newMsg.equals("offline")) {
                            receivingMessages = false;
                            connectSuccess = false;
                            Log.d(TAG, "Sensor went offline");
                        }
                        else if (newMsg.equals("started")) {

                        }
                        else {
                            message = newMsg;
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Delivery Completed Topic" + topic);
                    deliveryStatus = true;
                    if(topic.equals(MqttConnection.TOPIC_TARE))
                        Toast.makeText(context, "Recalibration Successful", Toast.LENGTH_SHORT).show();

                }

            });
        }catch(MqttException e) {
            e.printStackTrace();
        }

    }

    public void publish(String payload) {
        Log.d(TAG, "Publishing Topic:" + topic + "Msg:" + payload);
        byte[] encodedPayload = new byte[0];
        try{
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage msg = new MqttMessage(encodedPayload);
            client.publish(topic, msg);
        } catch(UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {

        Log.d(TAG, "Disconnecting Mqtt ... ");
        tryingToDisconnect = true;
        try {
            client.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void setWill(String lwtMessage, int qos, boolean retained) {
        byte[] bytePayload = lwtMessage.getBytes(StandardCharsets.UTF_8);
        options.setWill(topic, bytePayload, qos, retained);
        if(connectSuccess || receivingMessages) {
            connectAndSubscribe();
        }
    }

    public boolean isDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(boolean val) {
        deliveryStatus = val;
    }

    public boolean isConnectSuccess() {
        return connectSuccess;
    }

    public boolean isReceivingMessages() {
        return receivingMessages;
    }

    public String getMessage() {
        return message;
    }

}
