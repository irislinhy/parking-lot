package com.example.aliyunapplication;

import android.app.Application;
import android.util.Log;

public class ParkingApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 MQTT，传入 this（Application Context）
        HiveMQUtils.init(this, new HiveMQUtils.HiveMQCallback() {
            @Override
            public void onConnect() {
                Log.d("ParkingApp", "onConnect triggered");
                HiveMQUtils.subscribe("parking/status");
                Log.d("ParkingApp", "Subscribed to parking/status");
            }

            @Override
            public void onDisconnect(Throwable cause) {
                // 重连逻辑可选
            }

            @Override
            public void onMessage(String topic, String payload) {
                // 在这里处理收到的 MQTT 消息，可发送广播或通知界面更新
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
        HiveMQUtils.connect();  // 发起连接
    }
}