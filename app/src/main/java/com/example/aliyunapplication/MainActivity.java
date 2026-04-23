package com.example.aliyunapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;  // 添加这一行
import android.os.Looper;
import android.util.Log;  // 新增
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;  // 新增
import java.util.ArrayList;
import java.util.Date;  // 新增
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;  // 新增

public class MainActivity extends AppCompatActivity {

    private TextView carnum, cartime, money, tvFreeCount;
    private TextView hySlot1, hySlot2, playSlot1, playSlot2, moneySlot1, moneySlot2;
    private EditText highesthumiEditText1;
    private Spinner provinceSpinner, parkingSpinner;
    private String provinceCode;
    private int selectedParam = 0;
    private HashMap<String, String> provinceCodes;

    private ParkingSlot slot1 = new ParkingSlot(1, "车位1");
    private ParkingSlot slot2 = new ParkingSlot(2, "车位2");
    private List<ParkingSlot> slotList = new ArrayList<>();

    private ParkingMapView parkingMap;
    private Handler mHandler;
    private Runnable timerUpdate;

    private static final String RECORD_PREF = "ParkRecords";
    private SharedPreferences recordPref;
    private Button btnEntryGuide, btnExitGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordPref = getSharedPreferences(RECORD_PREF, MODE_PRIVATE);

        carnum = findViewById(R.id.hy);
        cartime = findViewById(R.id.play);
        money = findViewById(R.id.play1);
        hySlot1 = findViewById(R.id.hy_slot1);
        hySlot2 = findViewById(R.id.hy_slot2);
        playSlot1 = findViewById(R.id.play_slot1);
        playSlot2 = findViewById(R.id.play_slot2);
        moneySlot1 = findViewById(R.id.money_slot1);
        moneySlot2 = findViewById(R.id.money_slot2);
        highesthumiEditText1 = findViewById(R.id.highesthumiEditText1);
        provinceSpinner = findViewById(R.id.provinceSpinner);
        parkingSpinner = findViewById(R.id.parking_spinner);
        tvFreeCount = findViewById(R.id.tv_free_count);
        parkingMap = findViewById(R.id.parking_map);
        btnEntryGuide = findViewById(R.id.btn_entry_guide);
        btnExitGuide = findViewById(R.id.btn_exit_guide);

        slotList.add(slot1);
        slotList.add(slot2);
        parkingMap.setSlots(slotList);

        mHandler = new Handler(Looper.getMainLooper());
        timerUpdate = new Runnable() {
            @Override
            public void run() {
                updateSlotViews();
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.post(timerUpdate);

        initProvinceData();
        initSpinners();

        btnEntryGuide.setOnClickListener(v -> showGuideDialog(0));
        btnExitGuide.setOnClickListener(v -> showGuideDialog(1));

        // MQTT Handler — 支持 OneNET 物模型格式
        HiveMQUtils.setHandler(new Handler(Looper.getMainLooper(), msg -> {
            if (msg.what == 3 && msg.obj != null) {
                String message = msg.obj.toString();
                try {
                    JSONObject json = new JSONObject(message);
                    // 兼容物模型格式和普通格式
                    JSONObject params = json.optJSONObject("params");
                    if (params == null) params = json;

                    if (params.has("stall_1")) {
                        JSONObject field = params.optJSONObject("stall_1");
                        int status = field != null ? field.getInt("value") : params.getInt("stall_1");
                        slot1.status = status;
                        if (status == ParkingSlot.STATUS_OCCUPIED) slot1.startTs = System.currentTimeMillis();
                    }
                    if (params.has("stall_2")) {
                        JSONObject field = params.optJSONObject("stall_2");
                        int status = field != null ? field.getInt("value") : params.getInt("stall_2");
                        slot2.status = status;
                        if (status == ParkingSlot.STATUS_OCCUPIED) slot2.startTs = System.currentTimeMillis();
                    }
                    if (params.has("number")) {
                        JSONObject field = params.optJSONObject("number");
                        String plate = field != null ? field.getString("value") : params.getString("number");
                        slot1.plate = plate;
                    }
                    updateSlotViews();
                    parkingMap.invalidate();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }));
    }

    // 将普通 JSONObject 包装成 OneNET 物模型格式
    private String wrapPayload(JSONObject params) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("id", String.valueOf(System.currentTimeMillis()));
        root.put("version", "1.0");
        JSONObject wrapped = new JSONObject();
        Iterator<String> keys = params.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject field = new JSONObject();
            field.put("value", params.get(key));
            wrapped.put(key, field);
        }
        root.put("params", wrapped);
        return root.toString();
    }

    private void showGuideDialog(int type) {
        int targetSlot = -1;
        if (type == 0) {
            if (slot1.status == ParkingSlot.STATUS_EMPTY) targetSlot = 1;
            else if (slot2.status == ParkingSlot.STATUS_EMPTY) targetSlot = 2;
            if (targetSlot == -1) {
                Toast.makeText(this, "没有空闲车位", Toast.LENGTH_SHORT).show();
                return;
            }
            showParkingGuide(type, targetSlot);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("选择您的车位");
            String[] options = new String[]{"车位1", "车位2"};
            builder.setItems(options, (dialog, which) -> {
                int selected = which + 1;
                if ((selected == 1 && (slot1.status == ParkingSlot.STATUS_OCCUPIED || slot1.status == ParkingSlot.STATUS_PAY)) ||
                        (selected == 2 && (slot2.status == ParkingSlot.STATUS_OCCUPIED || slot2.status == ParkingSlot.STATUS_PAY))) {
                    showParkingGuide(type, selected);
                } else {
                    Toast.makeText(this, "该车位没有您的车辆", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        }
    }

    private void showParkingGuide(int type, int slot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_parking_guide, null);
        ParkingGuideView guideView = dialogView.findViewById(R.id.guide_view);
        TextView tvGuideText = dialogView.findViewById(R.id.tv_guide_text);
        guideView.setMode(type, slot);
        if (type == 0) {
            tvGuideText.setText("入场引导：请前往车位 " + slot + "，沿红色路线行驶。");
        } else {
            tvGuideText.setText("出场引导：请从车位 " + slot + " 驶向出口，沿红色路线行驶。");
        }
        builder.setView(dialogView);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }

    private void updateSlotViews() {
        parkingMap.setSlots(slotList);
        int freeCount = 0;
        for (ParkingSlot slot : slotList) {
            if (slot.status == ParkingSlot.STATUS_EMPTY) freeCount++;
        }
        tvFreeCount.setText("剩余车位：" + freeCount);

        hySlot1.setText("车位1：" + slot1.statusText() + " " + (slot1.plate.isEmpty() ? "无车牌" : slot1.plate));
        hySlot2.setText("车位2：" + slot2.statusText() + " " + (slot2.plate.isEmpty() ? "无车牌" : slot2.plate));
        String time1 = ParkUtil.formatTime(slot1.getDurationSeconds());
        String time2 = ParkUtil.formatTime(slot2.getDurationSeconds());
        playSlot1.setText("车位1时长：" + time1);
        playSlot2.setText("车位2时长：" + time2);
        moneySlot1.setText("车位1费用：" + calcFee(slot1.getDurationSeconds()) + " 元");
        moneySlot2.setText("车位2费用：" + calcFee(slot2.getDurationSeconds()) + " 元");

        if (slot1.status == ParkingSlot.STATUS_OCCUPIED || slot1.status == ParkingSlot.STATUS_PAY) {
            carnum.setText(slot1.plate);
            cartime.setText(time1);
            money.setText(calcFee(slot1.getDurationSeconds()) + " 元");
        } else if (slot2.status == ParkingSlot.STATUS_OCCUPIED || slot2.status == ParkingSlot.STATUS_PAY) {
            carnum.setText(slot2.plate);
            cartime.setText(time2);
            money.setText(calcFee(slot2.getDurationSeconds()) + " 元");
        } else {
            carnum.setText("暂无车辆");
            cartime.setText("00:00:00");
            money.setText("0 元");
        }
    }

    private int calcFee(long seconds) {
        long minutes = seconds / 60;
        if (minutes <= 0) return 0;
        int base = 10;
        if (minutes <= 60) return base;
        long extra = (minutes - 60 + 29) / 30;
        return Math.min(50, (int)(base + extra * 5));
    }

    public void onSendButtonClicked(View view) {
        String car = highesthumiEditText1.getText().toString().trim();
        if (car.isEmpty()) {
            Toast.makeText(this, "请输入车牌号", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject obj = new JSONObject();
            switch (selectedParam) {
                case 0:
                    obj.put("stall_1", ParkingSlot.STATUS_RESERVED);
                    slot1.status = ParkingSlot.STATUS_RESERVED;
                    slot1.plate = car;
                    break;
                case 1:
                    obj.put("stall_2", ParkingSlot.STATUS_RESERVED);
                    slot2.status = ParkingSlot.STATUS_RESERVED;
                    slot2.plate = car;
                    break;
                case 2:
                    obj.put("stall_1", ParkingSlot.STATUS_OCCUPIED);
                    slot1.status = ParkingSlot.STATUS_OCCUPIED;
                    slot1.startTs = System.currentTimeMillis();
                    slot1.plate = car;
                    break;
                case 3:
                    obj.put("stall_2", ParkingSlot.STATUS_OCCUPIED);
                    slot2.status = ParkingSlot.STATUS_OCCUPIED;
                    slot2.startTs = System.currentTimeMillis();
                    slot2.plate = car;
                    break;
                case 4:
                    showPaymentDialog(slot1);
                    return;
                case 5:
                    showPaymentDialog(slot2);
                    return;
            }
            obj.put("font", provinceCode);
            obj.put("number", car);
            updateSlotViews();
            HiveMQUtils.publish(wrapPayload(obj));
            Toast.makeText(this, "操作已发送", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showPaymentDialog(ParkingSlot slot) {
        if (slot.status != ParkingSlot.STATUS_OCCUPIED && slot.status != ParkingSlot.STATUS_PAY) {
            Toast.makeText(this, "该车位没有需要缴费的车辆", Toast.LENGTH_SHORT).show();
            return;
        }
        long seconds = slot.getDurationSeconds();
        int fee = calcFee(seconds);
        new AlertDialog.Builder(this)
                .setTitle("在线缴费")
                .setMessage("车牌：" + slot.plate + "\n停车时长：" + ParkUtil.formatTime(seconds) + "\n应缴金额：" + fee + "元\n确认支付？")
                .setPositiveButton("确认支付", (dialog, which) -> {
                    Toast.makeText(this, "支付成功！", Toast.LENGTH_SHORT).show();
                    saveParkRecord(slot.plate, ParkUtil.formatTime(seconds), fee);
                    try {
                        JSONObject obj = new JSONObject();
                        if (slot.id == 1) obj.put("stall_1", ParkingSlot.STATUS_EMPTY);
                        else obj.put("stall_2", ParkingSlot.STATUS_EMPTY);
                        obj.put("pay_success", true);
                        HiveMQUtils.publish(wrapPayload(obj));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    slot.reset();
                    updateSlotViews();
                    parkingMap.invalidate();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveParkRecord(String plate, String duration, double fee) {
    // 1. 本地保存
    String key = String.valueOf(System.currentTimeMillis());
    String record = plate + "|" + duration + "|" + fee;
    SharedPreferences.Editor editor = recordPref.edit();
    editor.putString(key, record);
    editor.apply();
    
    // 2. 同步到后端
    UserSessionManager session = new UserSessionManager(this);
    String username = session.getUsername();
    
    if (username.isEmpty()) {
        Log.w("MainActivity", "用户未登录");
        return;
    }
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    String parkingTime = sdf.format(new Date());
    
    ApiClient.saveParkingRecord(username, plate, parkingTime, duration, fee, 
        new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                Log.d("MainActivity", "记录已同步到云端");
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "同步失败: " + error);
            }
        });
    }

    public void onDebugOccupy1(View view) { debugSetSlot(slot1, ParkingSlot.STATUS_OCCUPIED, "调试车1"); }
    public void onDebugFree1(View view) { debugSetSlot(slot1, ParkingSlot.STATUS_EMPTY, ""); }
    public void onDebugSettle1(View view) { showPaymentDialog(slot1); }
    public void onDebugOccupy2(View view) { debugSetSlot(slot2, ParkingSlot.STATUS_OCCUPIED, "调试车2"); }
    public void onDebugFree2(View view) { debugSetSlot(slot2, ParkingSlot.STATUS_EMPTY, ""); }
    public void onDebugSettle2(View view) { showPaymentDialog(slot2); }

    private void debugSetSlot(ParkingSlot slot, int status, String plate) {
        slot.status = status;
        slot.plate = plate;
        if (status == ParkingSlot.STATUS_OCCUPIED) {
            slot.startTs = System.currentTimeMillis();
        } else {
            slot.startTs = 0;
        }
        updateSlotViews();
        try {
            JSONObject msg = new JSONObject();
            if (slot == slot1) msg.put("stall_1", status);
            else msg.put("stall_2", status);
            msg.put("number", plate);
            HiveMQUtils.publish(wrapPayload(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initProvinceData() {
        provinceCodes = new HashMap<>();
        provinceCodes.put("京", "4eac");
        provinceCodes.put("沪", "9c81");
        provinceCodes.put("粤", "7ca4");
        provinceCodes.put("苏", "82cf");
    }

    private void initSpinners() {
        ArrayAdapter<String> adPro = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, provinceCodes.keySet().toArray(new String[0]));
        provinceSpinner.setAdapter(adPro);
        provinceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = (String) parent.getItemAtPosition(position);
                provinceCode = provinceCodes.get(name);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] opt = {"车位1预约","车位2预约","车位1占用","车位2占用","车位1缴费","车位2缴费"};
        ArrayAdapter<String> adOpt = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opt);
        parkingSpinner.setAdapter(adOpt);
        parkingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedParam = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public void goBack(View view) {
        finish();
    }
}