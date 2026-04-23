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

import org.json.JSONObject;  // 新增

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

        // ========== 修改后的注册逻辑 ==========
        btnRegister.setOnClickListener(v -> {
            String username = etRegUsername.getText().toString().trim();
            String phone = etRegPhone.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            RadioButton rbUser = findViewById(R.id.rb_user);
            String role = rbUser.isChecked() ? "user" : "admin";

            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "信息不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 先调用远程 API 注册
            ApiClient.register(username, password, phone, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    // 远程注册成功后，本地也保存一份
                    LocalUserUtil.register(RegisterActivity.this, username, password, role);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // 网络失败时的备选方案
                        boolean success = LocalUserUtil.register(RegisterActivity.this, username, password, role);
                        if (success) {
                            Toast.makeText(RegisterActivity.this, "本地注册成功（网络不可用）", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "注册失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });

        // 返回登录
        tvBackLogin.setOnClickListener(v -> finish());
    }
}