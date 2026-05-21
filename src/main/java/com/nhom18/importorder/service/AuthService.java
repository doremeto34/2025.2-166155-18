package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IUserDAO;
import com.nhom18.importorder.dao.impl.SQLiteUserDAO;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.util.PasswordHasher;
import com.nhom18.importorder.util.SessionManager;

public class AuthService {
    private final IUserDAO userDAO;

    public AuthService() {
        // Sử dụng SQLiteUserDAO (có thể cấu hình đổi sang Supabase dễ dàng bằng Factory sau này)
        this.userDAO = new SQLiteUserDAO();
    }

    public boolean login(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return false;
        }

        User user = userDAO.getByUsername(username.trim());
        if (user == null || !user.isActive()) {
            return false;
        }

        // Băm mật khẩu nhập vào bằng SHA-256 để so sánh với passwordHash trong DB
        boolean match = PasswordHasher.verifyPassword(password, user.getPasswordHash());
        if (match) {
            SessionManager.getInstance().setCurrentUser(user);
            return true;
        }
        return false;
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }
}
