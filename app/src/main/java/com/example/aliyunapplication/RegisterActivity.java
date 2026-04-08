package com.example.aliyunapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegUsername, etRegPhone, etRegPassword;
    private ImageView ivToggleRegPwd;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        etRegUsername = findViewById(R.id.et_reg_username);
        etRegPhone = findViewById(R.id.et_reg_phone);
        etRegPassword = findViewById(R.id.et_reg_password);
        ivToggleRegPwd = findViewById(R.id.iv_toggle_reg_pwd);
        TextView tvBackLogin = findViewById(R.id.tv_back_login);
        View btnRegister = findViewById(R.id.btn_register);

        // 密码小眼睛
        ivToggleRegPwd.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etRegPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etRegPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etRegPassword.setSelection(etRegPassword.getText().length());
        });

        // 注册
        btnRegister.setOnClickListener(v -> {
            String username = etRegUsername.getText().toString().trim();
            String phone = etRegPhone.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            // 获取角色
            RadioButton rbUser = findViewById(R.id.rb_user);
            String role = rbUser.isChecked() ? "user" : "admin";

            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "信息不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用注册方法，传入 Context
            boolean success = LocalUserUtil.register(this, username, password, role);
            if (success) {
                Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
                finish(); // 返回登录页
            } else {
                Toast.makeText(this, "用户名已存在", Toast.LENGTH_SHORT).show();
            }
        });

        // 返回登录
        tvBackLogin.setOnClickListener(v -> finish());
    }
}