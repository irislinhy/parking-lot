package com.example.aliyunapplication;

public class ParkingSlot {
    public static final int STATUS_EMPTY = 0;
    public static final int STATUS_RESERVED = 1;
    public static final int STATUS_OCCUPIED = 2;
    public static final int STATUS_PAY = 3;

    public int id;
    public String title;
    public int status; // 0空,1预约,2占用,3待缴费
    public String plate;
    public long startTs;
    public long lastUpdateTs;

    public ParkingSlot(int id, String title) {
        this.id = id;
        this.title = title;
        this.status = STATUS_EMPTY;
        this.plate = "";
        this.startTs = 0;
        this.lastUpdateTs = System.currentTimeMillis();
    }

    public void reset() {
        status = STATUS_EMPTY;
        plate = "";
        startTs = 0;
        lastUpdateTs = System.currentTimeMillis();
    }

    public String statusText() {
        switch (status) {
            case STATUS_RESERVED:
                return "已预约";
            case STATUS_OCCUPIED:
                return "占用中";
            case STATUS_PAY:
                return "待缴费";
            case STATUS_EMPTY:
            default:
                return "未预约";
        }
    }

    public long getDurationSeconds() {
        if (startTs <= 0) return 0;
        return (System.currentTimeMillis() - startTs) / 1000;
    }
}
