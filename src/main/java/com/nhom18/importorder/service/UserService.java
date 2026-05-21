package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.IUserDAO;
import com.nhom18.importorder.dao.impl.SQLiteUserDAO;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.UserRole;
import com.nhom18.importorder.util.PasswordHasher;
import java.util.List;

public class UserService {
    private final IUserDAO userDAO;

    public UserService() {
        this.userDAO = new SQLiteUserDAO();
    }

    // Constructor phục vụ Mock Testing
    public UserService(IUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public List<User> getAllUsers() {
        return userDAO.getAll();
    }

    public User getUserById(int id) {
        return userDAO.getById(id);
    }

    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không hợp lệ!");
        }
        return userDAO.getByUsername(username.trim());
    }

    /**
     * Thêm mới tài khoản nhân viên vào hệ thống.
     * Mật khẩu ban đầu sẽ được băm bằng SHA-256 trước khi lưu.
     */
    public void createUser(User user, String plainPassword) {
        validateUser(user);
        
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu ban đầu không được để trống!");
        }

        // Kiểm tra xem tên đăng nhập đã tồn tại chưa
        User existing = userDAO.getByUsername(user.getUsername().trim());
        if (existing != null) {
            throw new IllegalArgumentException("Tên đăng nhập '" + user.getUsername() + "' đã tồn tại trên hệ thống!");
        }

        // Băm mật khẩu
        String hash = PasswordHasher.hashPassword(plainPassword.trim());
        user.setPasswordHash(hash);
        user.setActive(true); // Mặc định tài khoản tạo mới sẽ kích hoạt

        userDAO.insert(user);
    }

    /**
     * Cập nhật thông tin cơ bản của tài khoản nhân viên (Họ tên, Vai trò, Mã Site, Trạng thái).
     * Mật khẩu cũ được giữ nguyên.
     */
    public void updateUser(User user) {
        validateUser(user);

        User existing = userDAO.getById(user.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Tài khoản nhân viên không tồn tại!");
        }

        // Giữ nguyên mật khẩu cũ trong CSDL
        user.setPasswordHash(existing.getPasswordHash());

        userDAO.update(user);
    }

    /**
     * Đặt lại mật khẩu mới cho tài khoản nhân viên.
     * Mật khẩu mới được băm bằng SHA-256.
     */
    public void changePassword(int userId, String newPlainPassword) {
        if (newPlainPassword == null || newPlainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống!");
        }

        User existing = userDAO.getById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("Tài khoản nhân viên không tồn tại!");
        }

        String hash = PasswordHasher.hashPassword(newPlainPassword.trim());
        existing.setPasswordHash(hash);

        userDAO.update(existing);
    }

    /**
     * Bật/Tắt trạng thái hoạt động của tài khoản nhân viên.
     * Ràng buộc logic an toàn: Không cho phép tự khóa tài khoản của chính mình.
     */
    public void toggleUserActiveStatus(int userId, int currentLoggedInUserId) {
        if (userId == currentLoggedInUserId) {
            throw new IllegalStateException("Hệ thống bảo mật chặn hành vi tự ngừng hoạt động tài khoản của chính bạn!");
        }

        User existing = userDAO.getById(userId);
        if (existing == null) {
            throw new IllegalArgumentException("Tài khoản nhân viên không tồn tại!");
        }

        existing.setActive(!existing.isActive());
        userDAO.update(existing);
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Thông tin tài khoản không được trống!");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        if (user.getUsername().contains(" ")) {
            throw new IllegalArgumentException("Tên đăng nhập không được chứa khoảng trắng!");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên nhân viên không được để trống!");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Vai trò của nhân viên không được để trống!");
        }

        // Nếu vai trò là SITE, bắt buộc phải có liên kết siteCode
        if (user.getRole() == UserRole.SITE) {
            if (user.getSiteCode() == null || user.getSiteCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Vai trò Đại Diện Site yêu cầu phải liên kết với một mã Site đối tác!");
            }
        } else {
            // Không phải vai trò SITE thì xóa liên kết mã site
            user.setSiteCode(null);
        }
    }
}
