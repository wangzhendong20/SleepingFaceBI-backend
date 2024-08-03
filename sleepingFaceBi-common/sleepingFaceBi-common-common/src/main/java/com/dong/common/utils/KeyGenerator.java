package com.dong.common.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class KeyGenerator {

    // 生成AK（可以使用UUID）
    public static String generateAccessKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 生成SK（使用SHA-256哈希）
    public static String generateSecretKey() throws NoSuchAlgorithmException {
        // 随机生成一个字符串
        String randomString = generateRandomString();

//        // 使用SHA-256生成哈希
//        MessageDigest digest = MessageDigest.getInstance("SHA-256");
//        byte[] hash = digest.digest(randomString.getBytes(StandardCharsets.UTF_8));
//
//        // 将哈希结果进行Base64编码
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        return randomString;
    }

    // 生成随机字符串（用于生成SK的输入）
    private static String generateRandomString() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256-bit key
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static void main(String[] args) {
        try {
            String ak = generateAccessKey();
            String sk = generateSecretKey();

            System.out.println("Generated AK: " + ak);
            System.out.println("Generated SK: " + sk);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
