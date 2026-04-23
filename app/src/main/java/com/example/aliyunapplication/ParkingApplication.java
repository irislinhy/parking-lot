package com.example.aliyunapplication;

import android.app.Application;
import android.util.Log;

public class ParkingApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HiveMQUtils.init(this, new HiveMQUtils.HiveMQCallback() {
            @Override
            public void onConnect() {
                Log.d("ParkingApp", "Connected to OneNET");
            }

            @Override
            public void onDisconnect(Throwable cause) {
                Log.w("ParkingApp", "Disconnected", cause);
            }

            @Override
            public void onMessage(String topic, String payload) {
                Log.d("ParkingApp", "Message: " + payload);
            }

            @Override
            public void onError(Throwable error) {
                Log.e("ParkingApp", "Error", error);
            }
        });
        HiveMQUtils.connect();
    }
}