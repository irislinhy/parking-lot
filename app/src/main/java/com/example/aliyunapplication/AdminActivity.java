package com.example.aliyunapplication;

import android.content.Intent;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";

    private TextView stall1, stall2, allMoney, allCount, tvTodayIncome, tvTodayCount;
    private TextView tvTotalUsers;
    //private TextView tvSlot1Revenue, tvSlot2Revenue, tvTotalUsers;
    private int totalCount = 0;
    private double totalMoney = 0;

    private List<String> plateRecordList = new ArrayList<>();
    private ArrayAdapter<String> plateAdapter;
    private List<String> alarmList = new ArrayList<>();
    private ArrayAdapter<String> alarmAdapter;

    private List<String> userList = new ArrayList<>();
    //private List<String> blacklistData = new ArrayList<>();
    private List<String> recordList = new ArrayList<>();

    private BarChart barChart;
    //private PieChart pieChart;

    private PlayerView playerView;
    private ExoPlayer player;
    private String currentRtspUrl = "";
    private TextView overlayText;
    private Handler overlayHandler = new Handler();

    private String[] rtspUrls;
    private Handler mqttHandler;

    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;
    private List<JSONObject> userDataList = new ArrayList<>();


    private List<JSONObject> blacklistUserData = new ArrayList<>();  // 改名
    private List<String> blacklistDisplayList = new ArrayList<>();
    //private List<String> plateBlacklistData = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mqttHandler = new Handler(getMainLooper(), msg -> {
            if (msg.what == 3 && msg.obj != null) {
                String payload = msg.obj.toString();
                Log.d(TAG, "收到MQTT消息: " + payload);
                try {
                    JSONObject json = new JSONObject(payload);
                    JSONObject params = json.optJSONObject("params");
                    if (params == null) params = json;

                    if (params.has("stall_1")) {
                        JSONObject field = params.optJSONObject("stall_1");
                        int status = field != null ? field.getInt("value") : params.getInt("stall_1");
                        stall1.setText(status == 0 ? "空闲" : "占用");
                    }
                    if (params.has("stall_2")) {
                        JSONObject field = params.optJSONObject("stall_2");
                        int status = field != null ? field.getInt("value") : params.getInt("stall_2");
                        stall2.setText(status == 0 ? "空闲" : "占用");
                    }
                    if (params.has("plate")) {
                        JSONObject field = params.optJSONObject("plate");
                        String plate = field != null ? field.getString("value") : params.getString("plate");
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        plateRecordList.add(0, time + " - " + plate);
                        if (plateRecordList.size() > 20) plateRecordList.remove(plateRecordList.size() - 1);
                        plateAdapter.notifyDataSetChanged();
                        overlayText.setText("识别车牌: " + plate);
                        overlayText.setVisibility(View.VISIBLE);
                        overlayHandler.removeCallbacksAndMessages(null);
                        overlayHandler.postDelayed(() -> overlayText.setVisibility(View.GONE), 3000);
                    }
                    if (params.has("alarm")) {
                        JSONObject field = params.optJSONObject("alarm");
                        String alarmMsg = field != null ? field.getString("value") : params.getString("alarm");
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        alarmList.add(0, time + " - " + alarmMsg);
                        if (alarmList.size() > 20) alarmList.remove(alarmList.size() - 1);
                        alarmAdapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON解析错误", e);
                }
            }
            return true;
        });
        HiveMQUtils.setHandler(mqttHandler);

        initViews();
        initCharts();
        initVideoPlayer();

        // 加载所有数据
        loadAllData();

        // 设置定时刷新（每30秒）
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadStatsData();
                refreshHandler.postDelayed(this, 30000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 30000);
    }

    private void initViews() {
        stall1 = findViewById(R.id.stall1);
        stall2 = findViewById(R.id.stall2);
        allMoney = findViewById(R.id.allMoney);
        allCount = findViewById(R.id.allCount);
        tvTodayIncome = findViewById(R.id.tv_today_income);
        tvTodayCount = findViewById(R.id.tv_today_count);
        //tvSlot1Revenue = findViewById(R.id.tv_slot1_revenue);
        //tvSlot2Revenue = findViewById(R.id.tv_slot2_revenue);
        tvTotalUsers = findViewById(R.id.tv_total_users);

        rtspUrls = new String[]{
                "rtsp://10.29.9.240:8080/h264_ulaw.sdp",
                "rtsp://10.29.9.241:8080/h264_ulaw.sdp"
        };

        playerView = findViewById(R.id.player_view);
        overlayText = findViewById(R.id.overlay_text);
        Button btnCamera1 = findViewById(R.id.btn_camera1);
        Button btnCamera2 = findViewById(R.id.btn_camera2);

        ListView lvPlate = findViewById(R.id.lv_plate_records);
        ListView lvAlarm = findViewById(R.id.lv_alarm_events);
        plateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, plateRecordList);
        alarmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alarmList);
        lvPlate.setAdapter(plateAdapter);
        lvAlarm.setAdapter(alarmAdapter);

        // 用户管理按钮
        Button btnUserManage = findViewById(R.id.btn_user_manage);
        btnUserManage.setOnClickListener(v -> showUserManageDialog());

        // 黑白名单按钮
        Button btnBlacklist = findViewById(R.id.btn_blacklist);
        btnBlacklist.setOnClickListener(v -> showBlacklistDialog());

        // 停车记录按钮
        Button btnViewRecords = findViewById(R.id.btn_view_records);
        btnViewRecords.setOnClickListener(v -> showRecordsDialog());

        Button btnLogout = findViewById(R.id.btn_admin_logout);
        btnLogout.setOnClickListener(v -> {
            UserSessionManager session = new UserSessionManager(AdminActivity.this);
            session.logout();
            startActivity(new Intent(AdminActivity.this, LoginActivity.class));
            finish();
        });

        // 刷新按钮
        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> loadAllData());
    }

    private void initVideoPlayer() {
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(50, 150, 50, 50)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
        playerView.setPlayer(player);
        playRtsp(rtspUrls[0]);
        currentRtspUrl = rtspUrls[0];

        Button btnCamera1 = findViewById(R.id.btn_camera1);
        Button btnCamera2 = findViewById(R.id.btn_camera2);

        btnCamera1.setOnClickListener(v -> {
            if (!currentRtspUrl.equals(rtspUrls[0])) {
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
    }

    private void loadAllData() {
        loadStatsData();
        loadUserList();
        //loadBlacklistData();
        loadAllRecords();
    }

    private void loadStatsData() {
        ApiClient.getStats(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    try {
                        JSONObject stats = result.getJSONObject("stats");

                        double totalRevenue = stats.optDouble("total_revenue", 0);
                        int totalCount = stats.optInt("total_count", 0);
                        double todayIncome = stats.optDouble("today_income", 0);
                        int todayParking = stats.optInt("today_parking", 0);
                        double slot1Revenue = stats.optDouble("slot1_revenue", 0);
                        double slot2Revenue = stats.optDouble("slot2_revenue", 0);

                        allMoney.setText(String.format(Locale.getDefault(), "¥%.2f", totalRevenue));
                        allCount.setText(String.valueOf(totalCount));
                        tvTodayIncome.setText(String.format(Locale.getDefault(), "¥%.2f", todayIncome));
                        tvTodayCount.setText(String.valueOf(todayParking));
                        //tvSlot1Revenue.setText(String.format(Locale.getDefault(), "¥%.2f", slot1Revenue));
                        //tvSlot2Revenue.setText(String.format(Locale.getDefault(), "¥%.2f", slot2Revenue));

                        // 更新图表
                        updateCharts(stats);

                        // 显示最近记录
                        JSONArray recent = stats.optJSONArray("recent_records");
                        if (recent != null) {
                            recordList.clear();
                            for (int i = 0; i < recent.length(); i++) {
                                JSONObject rec = recent.getJSONObject(i);
                                String record = rec.optString("plate") + " | " +
                                        rec.optString("duration") + " | ¥" +
                                        rec.optDouble("fee", 0);
                                recordList.add(record);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminActivity.this, "加载统计数据失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadUserList() {
        ApiClient.getUsers(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    try {
                        JSONArray users = result.getJSONArray("users");
                        userDataList.clear();
                        userList.clear();

                        for (int i = 0; i < users.length(); i++) {
                            JSONObject user = users.getJSONObject(i);
                            userDataList.add(user);

                            int isBlacklisted = user.optInt("is_blacklisted", 0);
                            String status = isBlacklisted == 1 ? " [已拉黑]" : "";
                            String userInfo = user.optString("username") + " | " +
                                    user.optString("phone", "无手机号") + status;
                            userList.add(userInfo);
                        }
                        tvTotalUsers.setText(String.valueOf(users.length()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "加载用户列表失败: " + error);
            }
        });
    }

    /*private void loadBlacklistData() {
        ApiClient.getBlacklist(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    try {
                        JSONArray blacklist = result.getJSONArray("blacklist");
                        JSONArray whitelist = result.getJSONArray("whitelist");
                        
                        plateBlacklistData.clear();
                        
                        for (int i = 0; i < blacklist.length(); i++) {
                            JSONObject item = blacklist.getJSONObject(i);
                            plateBlacklistData.add("🚫 黑名单: " + item.optString("plate"));
                        }
                        for (int i = 0; i < whitelist.length(); i++) {
                            JSONObject item = whitelist.getJSONObject(i);
                            plateBlacklistData.add("✅ 白名单: " + item.optString("plate"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "加载车牌黑白名单失败: " + error);
            }
        });
    }*/

    private void loadAllRecords() {
        ApiClient.getAllRecords(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    try {
                        JSONArray records = result.getJSONArray("records");
                        recordList.clear();
                        for (int i = 0; i < records.length() && i < 50; i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String record = rec.optString("username") + " | " +
                                    rec.optString("plate") + " | " +
                                    rec.optString("parking_time").substring(0, 16) + " | ¥" +
                                    rec.optDouble("fee", 0);
                            recordList.add(record);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "加载停车记录失败: " + error);
            }
        });
    }

    // ==================== 用户管理对话框 ====================
    private void showUserManageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("用户管理 (" + userList.size() + "人)");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, userList);

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        builder.setView(listView);
        builder.setPositiveButton("关闭", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            showUserActionDialog(position);
        });
    }


    private void deleteUser(int userId) {
        ApiClient.deleteUser(userId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminActivity.this, "用户已删除", Toast.LENGTH_SHORT).show();
                    loadUserList();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    // ==================== 黑白名单管理对话框 ====================
    private void showBlacklistDialog() {
        // 先显示加载中
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("黑名单管理");
        builder.setMessage("加载中...");
        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();
        
        // 异步加载数据
        ApiClient.getBlacklistUsers(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    try {
                        JSONArray users = result.getJSONArray("blacklist");
                        blacklistUserData.clear();
                        blacklistDisplayList.clear();
                        
                        if (users.length() == 0) {
                            new AlertDialog.Builder(AdminActivity.this)
                                    .setTitle("黑名单管理")
                                    .setMessage("暂无黑名单用户")
                                    .setPositiveButton("关闭", null)
                                    .setNeutralButton("刷新", (d, w) -> showBlacklistDialog())
                                    .show();
                        } else {
                            for (int i = 0; i < users.length(); i++) {
                                JSONObject user = users.getJSONObject(i);
                                blacklistUserData.add(user);
                                
                                String display = user.optString("username") + " | " + 
                                                user.optString("phone", "无手机号");
                                blacklistDisplayList.add(display);
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminActivity.this,
                                    android.R.layout.simple_list_item_1, blacklistDisplayList);
                            
                            // 创建新的 ListView
                            ListView listView = new ListView(AdminActivity.this);
                            listView.setAdapter(adapter);
                            
                            AlertDialog.Builder listBuilder = new AlertDialog.Builder(AdminActivity.this);
                            listBuilder.setTitle("黑名单管理 (" + users.length() + "人)");
                            listBuilder.setView(listView);
                            listBuilder.setPositiveButton("关闭", null);
                            listBuilder.setNeutralButton("刷新", (d, w) -> showBlacklistDialog());
                            
                            AlertDialog listDialog = listBuilder.create();
                            listDialog.show();
                            
                            listView.setOnItemClickListener((parent, view, position, id) -> {
                                listDialog.dismiss();
                                showBlacklistActionDialog(position);
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(AdminActivity.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(AdminActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    /*private void showBlacklistViewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("当前名单");

        if (blacklistUserData.isEmpty()) {
            builder.setMessage("暂无名单数据");
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_list_item_1, blacklistDisplayList);
            builder.setAdapter(adapter, null);
        }

        builder.setPositiveButton("关闭", null);
        builder.setNeutralButton("刷新", (dialog, which) -> loadBlacklistData());
        builder.show();
    }

    private void showAddToListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加车牌");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("输入车牌号，如 京A12345");
        builder.setView(input);

        String[] types = {"黑名单", "白名单"};
        final String[] selectedType = {"black"};

        builder.setSingleChoiceItems(types, 0, (dialog, which) -> {
            selectedType[0] = which == 0 ? "black" : "white";
        });

        builder.setPositiveButton("添加", (dialog, which) -> {
            String plate = input.getText().toString().trim();
            if (plate.isEmpty()) {
                Toast.makeText(this, "请输入车牌号", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiClient.addToList(plate, "管理员添加", selectedType[0], new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                        loadBlacklistData();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminActivity.this, "添加失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }*/

    private void showUserActionDialog(int position) {
        JSONObject user = userDataList.get(position);
        String username = user.optString("username");
        final int userId = user.optInt("id");
        final int isBlacklisted = user.optInt("is_blacklisted", 0);
        
        // 如果已经拉黑，只显示删除；否则显示拉黑和删除
        String[] options;
        if (isBlacklisted == 1) {
            options = new String[]{"删除用户"};  // 已拉黑的只能删除
        } else {
            options = new String[]{"拉黑", "删除用户"};
        }
        
        new AlertDialog.Builder(this)
                .setTitle("操作: " + username)
                .setItems(options, (dialog, which) -> {
                    if (isBlacklisted == 1) {
                        // 已拉黑：只有删除
                        if (which == 0) {
                            showDeleteConfirmDialog(username, userId);
                        }
                    } else {
                        // 未拉黑：拉黑或删除
                        if (which == 0) {
                            // 拉黑
                            new AlertDialog.Builder(this)
                                    .setTitle("确认拉黑")
                                    .setMessage("确定要拉黑用户 " + username + " 吗？")
                                    .setPositiveButton("确定", (d, w) -> {
                                        toggleBlacklist(userId, "add");
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else if (which == 1) {
                            // 删除
                            showDeleteConfirmDialog(username, userId);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 抽取删除确认对话框
    private void showDeleteConfirmDialog(String username, int userId) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除用户 " + username + " 吗？")
                .setPositiveButton("删除", (d, w) -> deleteUser(userId))
                .setNegativeButton("取消", null)
                .show();
    }




    private void toggleBlacklist(int userId, String action) {
        ApiClient.toggleBlacklist(userId, action, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminActivity.this, result.optString("message"), Toast.LENGTH_SHORT).show();
                    loadUserList();  // 刷新用户列表
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminActivity.this, "操作失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void loadBlacklistUsers() {
        ApiClient.getBlacklistUsers(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> {
                    try {
                        JSONArray users = result.getJSONArray("blacklist");
                        blacklistUserData.clear();
                        blacklistDisplayList.clear();
                        
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject user = users.getJSONObject(i);
                            blacklistUserData.add(user);
                            
                            String display = user.optString("username") + " | " + 
                                            user.optString("phone", "无手机号");
                            blacklistDisplayList.add(display);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "加载黑名单失败: " + error);
            }
        });
    }


    private void showBlacklistActionDialog(int position) {
        JSONObject user = blacklistUserData.get(position); 
        String username = user.optString("username");
        final int userId = user.optInt("id");
        
        new AlertDialog.Builder(this)
                .setTitle("操作: " + username)
                .setMessage("确定要将 " + username + " 移出黑名单吗？")
                .setPositiveButton("移出黑名单", (dialog, which) -> {
                    toggleBlacklist(userId, "remove");
                })
                .setNegativeButton("取消", null)
                .show();
    }


    // ==================== 停车记录对话框 ====================
    private void showRecordsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("停车记录 (最近50条)");

        if (recordList.isEmpty()) {
            builder.setMessage("暂无停车记录");
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, recordList);
            builder.setAdapter(adapter, null);
        }

        builder.setPositiveButton("关闭", null);
        builder.setNeutralButton("刷新", (dialog, which) -> loadAllRecords());
        builder.show();
    }

    // ==================== 图表 ====================
    private void initCharts() {
        barChart = findViewById(R.id.bar_chart);
        //pieChart = findViewById(R.id.pie_chart);

        barChart.getDescription().setEnabled(false);
        //pieChart.getDescription().setEnabled(false);
    }

    private void updateCharts(JSONObject stats) {
        try {
            // 柱状图 - 近7天数据
            JSONArray weeklyData = stats.getJSONArray("weekly_data");
            List<BarEntry> entries = new ArrayList<>();

            for (int i = 0; i < weeklyData.length(); i++) {
                JSONObject day = weeklyData.getJSONObject(i);
                entries.add(new BarEntry(i + 1, day.optInt("count", 0)));
            }

            BarDataSet dataSet = new BarDataSet(entries, "停车次数");
            dataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
            BarData barData = new BarData(dataSet);
            barChart.setData(barData);
            barChart.invalidate();

        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
    // ==================== 闸机控制 ====================
    public void openGate1(View v) { sendCmd(1); }
    public void openGate2(View v) { sendCmd(2); }
    public void closeGate1(View v) { sendCmd(3); }
    public void closeGate2(View v) { sendCmd(4); }
    public void closeEntry(View v) { sendCmd(5); }
    public void openEntry(View v) { sendCmd(6); }

    private void sendCmd(int type) {
        try {
            JSONObject params = new JSONObject();
            params.put("admin_cmd", type);
            HiveMQUtils.publish(wrapPayload(params));
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

    private void playRtsp(String url) {
        if (player == null) return;
        player.stop();
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        RtspMediaSource.Factory factory = new RtspMediaSource.Factory();
        factory.setTimeoutMs(2000);
        factory.setForceUseRtpTcp(true);
        RtspMediaSource mediaSource = factory.createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    public void goBack(View view) {
        finish();
    }
}