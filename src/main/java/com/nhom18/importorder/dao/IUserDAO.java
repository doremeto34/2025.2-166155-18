package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.User;
import java.util.List;

public interface IUserDAO {
    User getById(int id);
    User getByUsername(String username);
    List<User> getAll();
    void insert(User user);
    void update(User user);
}
