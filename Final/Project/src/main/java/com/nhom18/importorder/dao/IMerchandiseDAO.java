package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.Merchandise;
import java.util.List;

public interface IMerchandiseDAO {
    List<Merchandise> getAllActive();
    List<Merchandise> getAll();
    Merchandise getByCode(String code);
    void insert(Merchandise merchandise);
    void update(Merchandise merchandise);
    boolean isUsedInPendingRequests(String code);
}

