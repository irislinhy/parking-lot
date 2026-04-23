package com.example.aliyunapplication;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final String TAG = "ApiClient";
    
    // ========== 改成你的服务器IP地址 ==========
    private static final String BASE_URL = "http://172.27.80.1/"; // 改成你的IP
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(JSONObject result);
        void onError(String error);
    }

    public static void post(String endpoint, JSONObject data, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String fullUrl = BASE_URL + endpoint;
                Log.d(TAG, "请求URL: " + fullUrl);
                
                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String jsonData = data.toString();
                Log.d(TAG, "发送数据: " + jsonData);
                
                OutputStream os = conn.getOutputStream();
                os.write(jsonData.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "响应码: " + responseCode);
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    BufferedReader errorBr = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorBr.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorBr.close();
                    Log.e(TAG, "错误响应: " + errorResponse.toString());
                    
                    mainHandler.post(() -> callback.onError("HTTP " + responseCode + ": " + errorResponse.toString()));
                    return;
                }
                
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String responseStr = response.toString();
                Log.d(TAG, "服务器响应: " + responseStr);

                JSONObject result = new JSONObject(responseStr);
                mainHandler.post(() -> {
                    if (result.optBoolean("success", false)) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(result.optString("message", "请求失败"));
                    }
                });

            } catch (java.net.ConnectException e) {
                Log.e(TAG, "连接失败，请检查IP地址和网络", e);
                mainHandler.post(() -> callback.onError("无法连接服务器，请检查网络"));
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "连接超时", e);
                mainHandler.post(() -> callback.onError("连接超时"));
            } catch (Exception e) {
                Log.e(TAG, "网络请求失败", e);
                mainHandler.post(() -> callback.onError("网络错误: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ==================== 原有方法 ====================
    
    // 注册
    public static void register(String username, String password, String phone, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("password", password);
            data.put("phone", phone);
            data.put("email", "");
            post("register.php", data, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 登录
    public static void login(String username, String password, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("password", password);
            post("login.php", data, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 保存停车记录
    public static void saveParkingRecord(String username, String plate, String parkingTime, 
                                         String duration, double fee, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("plate", plate);
            data.put("parking_time", parkingTime);
            data.put("duration", duration);
            data.put("fee", fee);
            post("save_record.php", data, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // ==================== 新增的管理员方法 ====================

    // 获取所有用户
    public static void getUsers(ApiCallback callback) {
        try {
            post("get_users.php", new JSONObject(), callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 删除用户
    public static void deleteUser(int userId, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("user_id", userId);
            post("delete_user.php", data, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 获取黑白名单
    public static void getBlacklist(ApiCallback callback) {
        try {
            post("get_blacklist.php", new JSONObject(), callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 添加黑白名单
    public static void addToList(String plate, String reason, String type, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("plate", plate);
            data.put("reason", reason);
            data.put("type", type); // "black" 或 "white"
            post("add_blacklist.php", data, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 删除黑白名单
    public static void deleteFromList(int id, String type, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("type", type);
            post("delete_blacklist.php", data, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 获取统计数据
    public static void getStats(ApiCallback callback) {
        try {
            post("get_stats.php", new JSONObject(), callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 获取所有停车记录
    public static void getAllRecords(ApiCallback callback) {
        try {
            post("get_all_records.php", new JSONObject(), callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    // 加入/移出黑名单
    public static void toggleBlacklist(int userId, String action, ApiCallback callback) {
      try {
        JSONObject data = new JSONObject();
        data.put("user_id", userId);
        data.put("action", action);  // "add" 或 "remove"
        post("toggle_blacklist.php", data, callback);
        } 
      catch (Exception e) {
        callback.onError(e.getMessage());
        }
    }

    // 获取黑名单用户列表
    public static void getBlacklistUsers(ApiCallback callback) {
        try {
            post("get_blacklist_users.php", new JSONObject(), callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }


}