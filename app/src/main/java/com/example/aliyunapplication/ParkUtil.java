package com.example.aliyunapplication;

public class ParkUtil {
    public static String formatTime(long totalSec) {
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public static double calculateFee(long minutes) {
        return minutes * 0.1;
    }
}