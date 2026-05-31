package com.nhom18.importorder.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHasher {
    
    public static String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi thuật toán mã hóa SHA-256: " + e.getMessage());
        }
    }
    
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        String hashedPassword = hashPassword(password);
        return hashedPassword.equalsIgnoreCase(storedHash);
    }
}
