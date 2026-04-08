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

    // 修改为您的 MQTT Broker 地址（如需测试可用公共 broker）
    public static String SERVER = "tcp://broker.hivemq.com:1883";
    public static String SUB_TOPIC = "parking/status";
    public static String PUB_TOPIC = "parking/ctrl";

    private static MqttAsyncClient client;
    private static HiveMQCallback listener;
    private static Handler legacyHandler;
    private static boolean isConnecting = false;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 重连相关
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
            String clientId = MqttClient.generateClientId();
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
                        Log.d(TAG, "Forwarded to legacyHandler");
                    } else {
                        Log.w(TAG, "legacyHandler is null, message not delivered");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // no-op
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "init exception", e);
            if (listener != null) listener.onError(e);
        }
    }

    public static synchronized void connect() {
        if (client == null || isConnecting || client.isConnected()) return;
        isConnecting = true;

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT connected");
                    isConnecting = false;
                    reconnectAttempts = 0;
                    if (listener != null) listener.onConnect();
                    // 确保订阅在连接成功后立即执行
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
        int delay = BASE_DELAY_MS * (1 << reconnectAttempts); // 指数退避
        reconnectAttempts++;
        Log.i(TAG, "Scheduling reconnect attempt #" + reconnectAttempts + " in " + delay + "ms");
        mainHandler.postDelayed(() -> {
            Log.i(TAG, "Attempting reconnect #" + reconnectAttempts);
            connect();
        }, delay);
    }

    // 修改 subscribe() 无参方法
    public static void subscribe() {
        if (client == null || !client.isConnected()) return;
        try {
            client.subscribe(SUB_TOPIC, 1);  // 不带回调
            Log.i(TAG, "Subscribed to " + SUB_TOPIC);
        } catch (MqttException e) {
            Log.e(TAG, "subscribe failed", e);
        }
    }

    // 修改 subscribe(String topic) 方法
    public static void subscribe(String topic) {
        if (client == null || !client.isConnected()) return;
        try {
            client.subscribe(topic, 1);  // 不带回调
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
            if (client.isConnected()) {
                client.disconnect();
            }
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