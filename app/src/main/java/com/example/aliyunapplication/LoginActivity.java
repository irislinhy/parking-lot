package com.example.aliyunapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ImageView ivTogglePwd;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        ivTogglePwd = findViewById(R.id.iv_toggle_pwd);
        TextView tvToRegister = findViewById(R.id.tv_to_register);
        View btnLogin = findViewById(R.id.btn_login);

        // 密码显示/隐藏切换
        ivTogglePwd.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "账号密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 远程 API 登录
            ApiClient.login(username, password, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    Log.d("LoginActivity", "服务器返回: " + result.toString());
                    
                    try {
                        // 从服务器获取用户信息
                        JSONObject user = result.getJSONObject("user");
                        String role = user.optString("role", "user");
                        
                        Log.d("LoginActivity", "用户名: " + username + ", 角色: " + role);
                        
                        // 保存登录状态
                        UserSessionManager sessionManager = new UserSessionManager(LoginActivity.this);
                        sessionManager.login(username, role);

                        runOnUiThread(() -> {
                            if ("admin".equals(role)) {
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                Toast.makeText(LoginActivity.this, "管理员登录成功", Toast.LENGTH_SHORT).show();
                            } else {
                                startActivity(new Intent(LoginActivity.this, MenuActivity.class));
                                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            }
                            finish();
                        });
                    } catch (JSONException e) {
                        Log.e("LoginActivity", "解析失败", e);
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("LoginActivity", "登录失败: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "登录失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // 跳转到注册页面
        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}