package com.example.aliyunapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";

    private TextView stall1, stall2, allMoney, allCount;
    private int totalCount = 0;
    private double totalMoney = 0;

    // 全景监控列表
    private List<String> plateRecordList = new ArrayList<>();
    private ArrayAdapter<String> plateAdapter;
    private List<String> alarmList = new ArrayList<>();
    private ArrayAdapter<String> alarmAdapter;

    // 图表
    private BarChart barChart;
    private PieChart pieChart;

    // ExoPlayer 组件
    private PlayerView playerView;
    private ExoPlayer player;
    private String currentRtspUrl = "";
    private TextView overlayText;
    private Handler overlayHandler = new Handler();

    private String[] rtspUrls;

    // MQTT 消息处理器
    private Handler mqttHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // ========== 关键：立即初始化 Handler 并设置到 HiveMQUtils ==========
        mqttHandler = new Handler(getMainLooper(), msg -> {
            if (msg.what == 3 && msg.obj != null) {
                String payload = msg.obj.toString();
                Log.d(TAG, "收到MQTT消息: " + payload);
                try {
                    JSONObject obj = new JSONObject(payload);
                    // 车位状态更新
                    if (obj.has("stall_1")) {
                        stall1.setText(obj.getInt("stall_1") == 0 ? "空闲" : "占用");
                    }
                    if (obj.has("stall_2")) {
                        stall2.setText(obj.getInt("stall_2") == 0 ? "空闲" : "占用");
                    }
                    // 车牌识别记录
                    if (obj.has("plate")) {
                        String plate = obj.getString("plate");
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        String record = time + " - " + plate;
                        plateRecordList.add(0, record);
                        if (plateRecordList.size() > 20) plateRecordList.remove(plateRecordList.size() - 1);
                        plateAdapter.notifyDataSetChanged();

                        // 显示叠加文字
                        overlayText.setText("识别车牌: " + plate);
                        overlayText.setVisibility(View.VISIBLE);
                        overlayHandler.removeCallbacksAndMessages(null);
                        overlayHandler.postDelayed(() -> overlayText.setVisibility(View.GONE), 3000);
                    }
                    // 报警事件
                    if (obj.has("alarm")) {
                        String alarmMsg = obj.getString("alarm");
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        String event = time + " - " + alarmMsg;
                        alarmList.add(0, event);
                        if (alarmList.size() > 20) alarmList.remove(alarmList.size() - 1);
                        alarmAdapter.notifyDataSetChanged();

                        // 自动切换摄像头（如果消息包含 camera_id）
                        if (obj.has("camera_id")) {
                            int camId = obj.getInt("camera_id");
                            if (camId == 1 && rtspUrls.length > 0 && !currentRtspUrl.equals(rtspUrls[0])) {
                                playRtsp(rtspUrls[0]);
                                currentRtspUrl = rtspUrls[0];
                                Toast.makeText(AdminActivity.this, "报警触发，切换至摄像头1", Toast.LENGTH_SHORT).show();
                            } else if (camId == 2 && rtspUrls.length > 1 && !currentRtspUrl.equals(rtspUrls[1])) {
                                playRtsp(rtspUrls[1]);
                                currentRtspUrl = rtspUrls[1];
                                Toast.makeText(AdminActivity.this, "报警触发，切换至摄像头2", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON解析错误", e);
                }
            }
            return true;
        });
        HiveMQUtils.setHandler(mqttHandler);
        // ============================================================

        // 初始化视图
        stall1 = findViewById(R.id.stall1);
        stall2 = findViewById(R.id.stall2);
        allMoney = findViewById(R.id.allMoney);
        allCount = findViewById(R.id.allCount);

        // RTSP 地址配置（请修改为你的实际地址）
        rtspUrls = new String[]{
                "rtsp://10.29.9.240:8080/h264_ulaw.sdp",
                "rtsp://10.29.9.241:8080/h264_ulaw.sdp"
        };

        // 全景监控 UI
        playerView = findViewById(R.id.player_view);
        overlayText = findViewById(R.id.overlay_text);
        Button btnCamera1 = findViewById(R.id.btn_camera1);
        Button btnCamera2 = findViewById(R.id.btn_camera2);

        // 极低延迟 LoadControl 配置
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(50, 150, 50, 50)   // 最小缓冲50ms，最大150ms
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

// 创建 ExoPlayer 并设置 LoadControl
        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
        playerView.setPlayer(player);
        playRtsp(rtspUrls[0]);
        currentRtspUrl = rtspUrls[0];

        btnCamera1.setOnClickListener(v -> {
            if (!currentRtspUrl.equals(rtspUrls[0]))


            {
                playRtsp(rtspUrls[0]);
                currentRtspUrl = rtspUrls[0];
            }
        });
        btnCamera2.setOnClickListener(v -> {
            if (rtspUrls.length > 1 && !currentRtspUrl.equals(rtspUrls[1])) {
                playRtsp(rtspUrls[1]);
                currentRtspUrl = rtspUrls[1];
            }
        });

        // 列表视图
        ListView lvPlate = findViewById(R.id.lv_plate_records);
        ListView lvAlarm = findViewById(R.id.lv_alarm_events);
        plateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, plateRecordList);
        alarmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alarmList);
        lvPlate.setAdapter(plateAdapter);
        lvAlarm.setAdapter(alarmAdapter);

        // 图表
        barChart = findViewById(R.id.bar_chart);
        pieChart = findViewById(R.id.pie_chart);
        setupCharts();

        // 黑白名单管理按钮
        Button btnBlacklist = findViewById(R.id.btn_blacklist);
        btnBlacklist.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, BlacklistActivity.class)));

        // 退出登录按钮
        Button btnLogout = findViewById(R.id.btn_admin_logout);
        btnLogout.setOnClickListener(v -> {
            UserSessionManager session = new UserSessionManager(AdminActivity.this);
            session.logout();
            startActivity(new Intent(AdminActivity.this, LoginActivity.class));
            finish();
        });

        // 测试数据（正式使用时删除）
        plateRecordList.add("测试 - 京A12345");
        plateAdapter.notifyDataSetChanged();
        alarmList.add("测试 - 非法闯入");
        alarmAdapter.notifyDataSetChanged();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void playRtsp(String url) {
        if (player == null) return;
        player.stop();
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        RtspMediaSource.Factory factory = new RtspMediaSource.Factory();
        factory.setTimeoutMs(2000);                 // 连接超时 2 秒
        factory.setForceUseRtpTcp(true);            // 强制 TCP
        // 添加自定义低延迟选项（需要反射，可选）
        RtspMediaSource mediaSource = factory.createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private void setupCharts() {
        SharedPreferences recordPref = getSharedPreferences("ParkRecords", MODE_PRIVATE);
        Map<String, ?> allRecords = recordPref.getAll();

        long[] dailyCount = new long[7];
        double[] slotRevenue = new double[2];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        long today = Long.parseLong(sdf.format(new Date()));

        for (Map.Entry<String, ?> entry : allRecords.entrySet()) {
            String record = (String) entry.getValue();
            String[] parts = record.split("\\|");
            if (parts.length < 3) continue;
            long timestamp = Long.parseLong(entry.getKey());
            long recordDay = Long.parseLong(sdf.format(new Date(timestamp)));
            int dayDiff = (int) ((today - recordDay) / 100);
            if (dayDiff >= 0 && dayDiff < 7) dailyCount[6 - dayDiff]++;
            double fee = Double.parseDouble(parts[2]);
            if (parts[0].hashCode() % 2 == 0) slotRevenue[0] += fee;
            else slotRevenue[1] += fee;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) entries.add(new BarEntry(i + 1, (float) dailyCount[i]));
        BarDataSet dataSet = new BarDataSet(entries, "停车次数");
        dataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();

        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry((float) slotRevenue[0], "车位1"));
        pieEntries.add(new PieEntry((float) slotRevenue[1], "车位2"));
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "收入占比");
        pieDataSet.setColors(ContextCompat.getColor(this, R.color.teal_200),
                ContextCompat.getColor(this, R.color.purple_200));
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
    }

    // 远程控制方法
    public void openGate1(View v) { sendCmd(1); }
    public void openGate2(View v) { sendCmd(2); }
    public void closeGate1(View v) { sendCmd(3); }
    public void closeGate2(View v) { sendCmd(4); }
    public void closeEntry(View v) { sendCmd(5); }
    public void openEntry(View v) { sendCmd(6); }

    public void clearStats(View v) {
        totalCount = 0;
        totalMoney = 0;
        allCount.setText("0");
        allMoney.setText("0 元");
    }

    private void sendCmd(int type) {
        try {
            JSONObject json = new JSONObject();
            json.put("admin_cmd", type);
            HiveMQUtils.publish(json.toString());
            String msg = "";
            switch (type) {
                case 1: msg = "闸机1已开启"; break;
                case 2: msg = "闸机2已开启"; break;
                case 3: msg = "闸机1已关闭"; break;
                case 4: msg = "闸机2已关闭"; break;
                case 5: msg = "入口已关闭"; break;
                case 6: msg = "入口已开启"; break;
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        UserSessionManager session = new UserSessionManager(this);
        session.logout();
        startActivity(new Intent(AdminActivity.this, LoginActivity.class));
        finish();
        super.onBackPressed();
    }

    public void goBack(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }
}