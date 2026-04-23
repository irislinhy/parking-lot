//用户主菜单
package com.example.aliyunapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btn_park = findViewById(R.id.btn_park);
        Button btn_record = findViewById(R.id.btn_record);
        Button btn_logout = findViewById(R.id.btn_logout);

        btn_park.setOnClickListener(v -> {
            startActivity(new Intent(MenuActivity.this, MainActivity.class));
        });

        btn_record.setOnClickListener(v -> {
            startActivity(new Intent(MenuActivity.this, RecordActivity.class));
        });

        btn_logout.setOnClickListener(v -> {
            UserSessionManager session = new UserSessionManager(MenuActivity.this);
            session.logout();
            startActivity(new Intent(MenuActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        UserSessionManager session = new UserSessionManager(this);
        session.logout();
        startActivity(new Intent(MenuActivity.this, LoginActivity.class));
        finish();
    }
}