package com.nhom18.importorder.util;

import com.nhom18.importorder.model.entity.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        if (currentUser != null) {
            System.out.println("Phiên làm việc mới bắt đầu cho User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        } else {
            System.out.println("User đã đăng xuất.");
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }
}
