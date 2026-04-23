package com.example.aliyunapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class HiveMQUtils {
    private static final String TAG = "HiveMQUtils";

    private static final String PRODUCT_ID = "CY18BwtuRD";
    private static final String DEVICE_NAME = "parking_device";
    private static final String ACCESS_KEY = "cUZLdGRKRHJ4bW96Z09yb1hONW5FSmRWeXNBUUx2UXU=";

    public static final String SERVER = "tcp://studio-mqtt.heclouds.com:1883";
    
    public static final String SUB_TOPIC =  "$sys/CY18BwtuRD/parking_device/thing/property/post/reply";
    public static final String PUB_TOPIC = "$sys/CY18BwtuRD/parking_device/thing/property/post";

    private static MqttAsyncClient client;
    private static HiveMQCallback listener;
    private static Handler legacyHandler;
    private static boolean isConnecting = false;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int BASE_DELAY_MS = 2000;

    public interface HiveMQCallback {
        void onConnect();
        void onDisconnect(Throwable cause);
        void onMessage(String topic, String payload);
        void onError(Throwable error);
    }

    public static void init(Context context, HiveMQCallback callback) {
        if (context == null) throw new IllegalArgumentException("context required");
        listener = callback;
        if (client != null) return;

        try {
            String clientId = DEVICE_NAME;
            client = new MqttAsyncClient(SERVER, clientId, null);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.w(TAG, "connectionLost", cause);
                    if (listener != null) listener.onDisconnect(cause);
                    reconnectWithBackoff();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "messageArrived (" + topic + "): " + payload);
                    if (listener != null) listener.onMessage(topic, payload);
                    if (legacyHandler != null) {
                        Message msg = Message.obtain();
                        msg.what = 3;
                        msg.obj = payload;
                        legacyHandler.sendMessage(msg);
                    } else {
                        Log.w(TAG, "legacyHandler is null, message not delivered");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });
        } catch (MqttException e) {
            Log.e(TAG, "init exception", e);
            if (listener != null) listener.onError(e);
        }
    }

    public static synchronized void connect() {
        if (client == null || isConnecting || client.isConnected()) return;
        isConnecting = true;

        String token = OneNetTokenUtil.generateToken(PRODUCT_ID, DEVICE_NAME, ACCESS_KEY);
        if (token == null) {
            Log.e(TAG, "Token generation failed");
            isConnecting = false;
            return;
        }

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            options.setUserName(PRODUCT_ID);
            options.setPassword(token.toCharArray());
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT connected to OneNET");
                    isConnecting = false;
                    reconnectAttempts = 0;
                    if (listener != null) listener.onConnect();
                    subscribe(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    isConnecting = false;
                    Log.e(TAG, "MQTT connect fail", exception);
                    if (listener != null) listener.onError(exception);
                    reconnectWithBackoff();
                }
            });
        } catch (MqttException e) {
            isConnecting = false;
            Log.e(TAG, "connect exception", e);
            if (listener != null) listener.onError(e);
            reconnectWithBackoff();
        }
    }

    private static void reconnectWithBackoff() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached, giving up.");
            return;
        }
        int delay = BASE_DELAY_MS * (1 << reconnectAttempts);
        reconnectAttempts++;
        Log.i(TAG, "Scheduling reconnect #" + reconnectAttempts + " in " + delay + "ms");
        mainHandler.postDelayed(() -> connect(), delay);
    }

    public static void subscribe(String topic) {
        if (client == null || !client.isConnected()) return;
        try {
            client.subscribe(topic, 1);
            Log.i(TAG, "Subscribed to " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "subscribe failed for " + topic, e);
        }
    }

    public static void publish(String message) {
        if (client == null || !client.isConnected()) {
            Log.w(TAG, "publish skipped, no connection");
            return;
        }
        try {
            MqttMessage mqttMsg = new MqttMessage(message.getBytes());
            mqttMsg.setQos(1);
            mqttMsg.setRetained(false);
            client.publish(PUB_TOPIC, mqttMsg);
        } catch (MqttException e) {
            Log.e(TAG, "publish failed", e);
            if (listener != null) listener.onError(e);
        }
    }

    public static void disconnect() {
        if (client == null) return;
        try {
            if (client.isConnected()) client.disconnect();
        } catch (MqttException e) {
            Log.w(TAG, "disconnect error", e);
        }
    }

    public static boolean isConnected() {
        return client != null && client.isConnected();
    }

    public static void setHandler(Handler h) {
        Log.d(TAG, "setHandler called, handler=" + h);
        legacyHandler = h;
    }
}