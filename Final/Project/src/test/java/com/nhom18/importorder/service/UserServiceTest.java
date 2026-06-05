package com.nhom18.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom18.importorder.dao.IUserDAO;
import com.nhom18.importorder.model.entity.User;
import com.nhom18.importorder.model.enums.UserRole;
import com.nhom18.importorder.util.PasswordHasher;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserServiceTest {

    private UserService userService;
    private MockUserDAO mockUserDAO;

    @BeforeEach
    public void setUp() {
        mockUserDAO = new MockUserDAO();
        userService = new UserService(mockUserDAO);
    }

    @Test
    public void testGetAllUsers() {
        User user1 = new User(1, "john", "hash1", "John Doe", UserRole.BPBH, null, true);
        User user2 = new User(2, "jane", "hash2", "Jane Doe", UserRole.BPQLK, null, true);
        mockUserDAO.users.add(user1);
        mockUserDAO.users.add(user2);

        List<User> result = userService.getAllUsers();
        assertEquals(2, result.size());
        assertEquals("john", result.get(0).getUsername());
        assertEquals("jane", result.get(1).getUsername());
    }

    @Test
    public void testGetUserById() {
        User user = new User(1, "john", "hash1", "John Doe", UserRole.BPBH, null, true);
        mockUserDAO.users.add(user);

        User found = userService.getUserById(1);
        assertNotNull(found);
        assertEquals("john", found.getUsername());

        User notFound = userService.getUserById(99);
        assertNull(notFound);
    }

    @Test
    public void testGetUserByUsername() {
        User user = new User(1, "john", "hash1", "John Doe", UserRole.BPBH, null, true);
        mockUserDAO.users.add(user);

        User found = userService.getUserByUsername("john");
        assertNotNull(found);
        assertEquals("John Doe", found.getFullName());

        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserByUsername("");
        });
    }

    @Test
    public void testCreateUserSuccess() {
        User user = new User();
        user.setUsername("alice");
        user.setFullName("Alice Smith");
        user.setRole(UserRole.BPBH);

        userService.createUser(user, "alicePass123");

        assertEquals(1, mockUserDAO.users.size());
        User created = mockUserDAO.users.get(0);
        assertEquals("alice", created.getUsername());
        assertEquals("Alice Smith", created.getFullName());
        assertTrue(created.isActive());
        assertNotNull(created.getPasswordHash());
        assertTrue(PasswordHasher.verifyPassword("alicePass123", created.getPasswordHash()));
    }

    @Test
    public void testCreateUserDuplicateUsernameThrowsException() {
        User existing = new User(1, "alice", "hash", "Alice", UserRole.BPBH, null, true);
        mockUserDAO.users.add(existing);

        User newUser = new User();
        newUser.setUsername("alice");
        newUser.setFullName("Alice Smith");
        newUser.setRole(UserRole.BPQLK);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(newUser, "newpassword");
        });
        assertTrue(exception.getMessage().contains("đã tồn tại"));
    }

    @Test
    public void testCreateUserSiteRoleValidation() {
        // Site role without site code should throw
        User siteUser = new User();
        siteUser.setUsername("site_rep");
        siteUser.setFullName("Site Representative");
        siteUser.setRole(UserRole.SITE);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(siteUser, "password");
        });

        // Site role with site code should succeed
        siteUser.setSiteCode("S001");
        userService.createUser(siteUser, "password");
        assertEquals(1, mockUserDAO.users.size());
        assertEquals("S001", mockUserDAO.users.get(0).getSiteCode());
    }

    @Test
    public void testCreateUserNonSiteRoleResetsSiteCode() {
        User bpqlkUser = new User();
        bpqlkUser.setUsername("bpqlk");
        bpqlkUser.setFullName("Warehouse Officer");
        bpqlkUser.setRole(UserRole.BPQLK);
        bpqlkUser.setSiteCode("S001"); // Irrelevant site code for non-SITE role

        userService.createUser(bpqlkUser, "password");
        assertEquals(1, mockUserDAO.users.size());
        assertNull(mockUserDAO.users.get(0).getSiteCode(), "siteCode must be reset to null for non-SITE role");
    }

    @Test
    public void testUpdateUserSuccess() {
        User existing = new User(1, "bob", PasswordHasher.hashPassword("bobPass"), "Bob Original", UserRole.BPBH, null, true);
        mockUserDAO.users.add(existing);

        User updateData = new User();
        updateData.setId(1);
        updateData.setUsername("bob");
        updateData.setFullName("Bob Updated");
        updateData.setRole(UserRole.BPDHQT);
        updateData.setActive(true);

        userService.updateUser(updateData);

        User updated = mockUserDAO.getById(1);
        assertEquals("Bob Updated", updated.getFullName());
        assertEquals(UserRole.BPDHQT, updated.getRole());
        assertTrue(PasswordHasher.verifyPassword("bobPass", updated.getPasswordHash()), "Password hash must be preserved");
    }

    @Test
    public void testChangePassword() {
        User user = new User(1, "bob", PasswordHasher.hashPassword("oldPass"), "Bob", UserRole.BPBH, null, true);
        mockUserDAO.users.add(user);

        userService.changePassword(1, "newPass456");

        User updated = mockUserDAO.getById(1);
        assertTrue(PasswordHasher.verifyPassword("newPass456", updated.getPasswordHash()));
        assertFalse(PasswordHasher.verifyPassword("oldPass", updated.getPasswordHash()));
    }

    @Test
    public void testToggleUserActiveStatusSuccess() {
        User user = new User(1, "bob", "hash", "Bob", UserRole.BPBH, null, true);
        mockUserDAO.users.add(user);

        userService.toggleUserActiveStatus(1, 99); // current logged in ID = 99
        assertFalse(mockUserDAO.getById(1).isActive());

        userService.toggleUserActiveStatus(1, 99);
        assertTrue(mockUserDAO.getById(1).isActive());
    }

    @Test
    public void testToggleUserActiveStatusSelfThrowsException() {
        User user = new User(1, "admin", "hash", "Admin", UserRole.ADMIN, null, true);
        mockUserDAO.users.add(user);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.toggleUserActiveStatus(1, 1); // target ID == logged in ID
        });
        assertTrue(exception.getMessage().contains("tự ngừng hoạt động"));
    }

    // --- Mock IUserDAO Implementation ---
    private static class MockUserDAO implements IUserDAO {
        final List<User> users = new ArrayList<>();
        private int idSequence = 1;

        @Override
        public User getById(int id) {
            return users.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
        }

        @Override
        public User getByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
        }

        @Override
        public List<User> getAll() {
            return users;
        }

        @Override
        public void insert(User user) {
            user.setId(idSequence++);
            users.add(user);
        }

        @Override
        public void update(User user) {
            User existing = getById(user.getId());
            if (existing != null) {
                existing.setUsername(user.getUsername());
                existing.setPasswordHash(user.getPasswordHash());
                existing.setFullName(user.getFullName());
                existing.setRole(user.getRole());
                existing.setSiteCode(user.getSiteCode());
                existing.setActive(user.isActive());
            }
        }
    }
}
