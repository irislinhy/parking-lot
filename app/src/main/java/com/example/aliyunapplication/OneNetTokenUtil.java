package com.example.aliyunapplication;

import android.util.Base64;
import android.util.Log;

import java.net.URLEncoder;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class OneNetTokenUtil {
    private static final String TAG = "OneNetTokenUtil";

    public static String generateToken(String productId, String deviceName, String accessKey) {
        try {
            // 1. 使用固定的超长过期时间，防止时间戳计算误差
            long expireTime = 9999999999L;

            // 2. 版本号与资源路径 (官方示例标准写法)
            String version = "2018-10-31";
            String res = "products/" + productId + "/devices/" + deviceName;
            String method = "sha1";

            // 3. 核心：构造待签名字符串 (必须严格按照这个格式)
            String stringToSign = expireTime + "\n" + method + "\n" + res + "\n" + version;

            Log.d(TAG, "待签名字符串: " + stringToSign.replace("\n", "\\n"));

            // 4. Base64 解码 AccessKey
            byte[] keyBytes = Base64.decode(accessKey, Base64.NO_WRAP);

            // 5. HmacSHA1 加密
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
            byte[] signBytes = mac.doFinal(stringToSign.getBytes("UTF-8"));

            // 6. 生成签名并进行 URL 安全编码
            String sign = Base64.encodeToString(signBytes, Base64.NO_WRAP);
            String encodedSign = URLEncoder.encode(sign, "UTF-8");
            String encodedRes = URLEncoder.encode(res, "UTF-8");

            // 7. 组装最终 Token
            String token = "version=" + version + "&res=" + encodedRes + "&et=" + expireTime
                    + "&method=" + method + "&sign=" + encodedSign;

            Log.d(TAG, "生成的Token: " + token);

            return token;
        } catch (Exception e) {
            Log.e(TAG, "Token生成失败", e);
            e.printStackTrace();
            return null;
        }
    }
}