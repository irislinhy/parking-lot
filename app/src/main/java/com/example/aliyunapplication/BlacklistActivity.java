package com.example.aliyunapplication;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlacklistActivity extends AppCompatActivity {
    private static final String PREF_NAME = "BlackWhiteList";
    private static final String KEY_BLACK = "black_list";
    private static final String KEY_WHITE = "white_list";
    private SharedPreferences prefs;
    private Set<String> blackSet, whiteSet;
    private ArrayAdapter<String> adapter;
    private List<String> displayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        blackSet = new HashSet<>(prefs.getStringSet(KEY_BLACK, new HashSet<>()));
        whiteSet = new HashSet<>(prefs.getStringSet(KEY_WHITE, new HashSet<>()));
        displayList = new ArrayList<>();

        ListView lv = findViewById(R.id.lv_blacklist);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lv.setAdapter(adapter);

        // 先刷新显示
        updateDisplayList();

        EditText etPlate = findViewById(R.id.et_plate);
        Spinner spinnerType = findViewById(R.id.spinner_type);
        Button btnAdd = findViewById(R.id.btn_add);

        btnAdd.setOnClickListener(v -> {
            String plate = etPlate.getText().toString().trim();
            if (plate.isEmpty()) {
                Toast.makeText(this, "请输入车牌号", Toast.LENGTH_SHORT).show();
                return;
            }
            // 获取选中的位置：0黑名单，1白名单
            int type = spinnerType.getSelectedItemPosition();
            if (type == 0) { // 黑名单
                blackSet.add(plate);
                whiteSet.remove(plate);
                Toast.makeText(this, "已添加到黑名单", Toast.LENGTH_SHORT).show();
            } else { // 白名单
                whiteSet.add(plate);
                blackSet.remove(plate);
                Toast.makeText(this, "已添加到白名单", Toast.LENGTH_SHORT).show();
            }
            saveAndRefresh();
            etPlate.setText("");
        });

        // 长按删除
        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            String item = displayList.get(position);
            // 提取车牌号（去掉前缀，前缀格式为 "黑名单: " 或 "白名单: "）
            String plate = item.substring(item.indexOf(":") + 2);
            new AlertDialog.Builder(this)
                    .setTitle("删除")
                    .setMessage("是否从名单中删除 " + plate + "?")
                    .setPositiveButton("删除", (dialog, which) -> {
                        blackSet.remove(plate);
                        whiteSet.remove(plate);
                        saveAndRefresh();
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    private void updateDisplayList() {
        displayList.clear();
        for (String plate : blackSet) {
            displayList.add("黑名单: " + plate);
        }
        for (String plate : whiteSet) {
            displayList.add("白名单: " + plate);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void saveAndRefresh() {
        prefs.edit()
                .putStringSet(KEY_BLACK, blackSet)
                .putStringSet(KEY_WHITE, whiteSet)
                .apply();
        updateDisplayList();
    }

    public void goBack(View view) {
        finish();
    }
}