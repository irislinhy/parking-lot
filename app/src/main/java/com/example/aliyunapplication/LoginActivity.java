package com.example.aliyunapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ImageView ivTogglePwd;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 不再自动登录，每次都显示登录界面

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        ivTogglePwd = findViewById(R.id.iv_toggle_pwd);
        TextView tvToRegister = findViewById(R.id.tv_to_register);
        View btnLogin = findViewById(R.id.btn_login);

        ivTogglePwd.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "账号密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用 LocalUserUtil.checkLogin 需要传入 Context
            String role = LocalUserUtil.checkLogin(LoginActivity.this, username, password);

            if (role == null) {
                Toast.makeText(LoginActivity.this, "账号或密码错误", Toast.LENGTH_SHORT).show();
                return;
            }

            UserSessionManager sessionManager = new UserSessionManager(LoginActivity.this);
            sessionManager.login(username, role);

            if ("admin".equals(role)) {
                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                Toast.makeText(LoginActivity.this, "管理员登录成功", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(LoginActivity.this, MenuActivity.class));
                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}