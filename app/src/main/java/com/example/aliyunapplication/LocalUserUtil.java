package com.example.aliyunapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalUserUtil {
    private static final String USER_PREF = "UserData";
    private static final String KEY_PREFIX_PWD = "pwd_";
    private static final String KEY_PREFIX_ROLE = "role_";

    // 注册时保存用户
    public static boolean register(Context context, String username, String password, String role) {
        SharedPreferences pref = context.getSharedPreferences(USER_PREF, Context.MODE_PRIVATE);
        if (pref.contains(KEY_PREFIX_PWD + username)) {
            return false; // 用户名已存在
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_PREFIX_PWD + username, password);
        editor.putString(KEY_PREFIX_ROLE + username, role);
        editor.apply();
        return true;
    }

    // 登录验证，返回角色（"user" 或 "admin"），失败返回 null
    public static String checkLogin(Context context, String username, String password) {
        SharedPreferences pref = context.getSharedPreferences(USER_PREF, Context.MODE_PRIVATE);
        String savedPwd = pref.getString(KEY_PREFIX_PWD + username, null);
        if (savedPwd == null || !savedPwd.equals(password)) {
            return null;
        }
        return pref.getString(KEY_PREFIX_ROLE + username, "user");
    }
}