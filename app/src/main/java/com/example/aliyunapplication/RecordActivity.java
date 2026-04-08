package com.example.aliyunapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Map;

public class RecordActivity extends AppCompatActivity {
    private ListView lvRecords;
    private ArrayList<String> recordList;
    private ArrayAdapter<String> adapter;
    public void goBack(View view) {
        finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        lvRecords = findViewById(R.id.lv_records);
        recordList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recordList);
        lvRecords.setAdapter(adapter);

        loadRecords();
    }

    private void loadRecords() {
        SharedPreferences prefs = getSharedPreferences("ParkRecords", MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        recordList.clear();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String record = (String) entry.getValue();
            // record 格式: 车牌|时长|费用
            String[] parts = record.split("\\|");
            if (parts.length == 3) {
                recordList.add("车牌：" + parts[0] + "  时长：" + parts[1] + "  费用：" + parts[2] + "元");
            }
        }
        adapter.notifyDataSetChanged();
    }
}