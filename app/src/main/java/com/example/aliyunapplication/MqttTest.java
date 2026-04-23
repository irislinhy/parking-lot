package com.example.aliyunapplication;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.*;

public class MqttTest {
    private static final String TAG = "MqttTest";
    private MqttAsyncClient client;

    public void testConnectAndSubscribe(Context context) {
        try {
            String server = "tcp://broker.hivemq.com:1883";
            String clientId = MqttClient.generateClientId();
            client = new MqttAsyncClient(server, clientId, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "Connected successfully");
                    try {
                        client.subscribe("parking/status", 1, (topic, msg) -> {
                            Log.i(TAG, "Received on topic: " + topic + " -> " + new String(msg.getPayload()));
                        });
                        Log.i(TAG, "Subscribed to parking/status");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connect failed", exception);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}