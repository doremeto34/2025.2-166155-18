package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.WarehouseReceipt;
import java.util.List;

public interface IWarehouseReceiptDAO {
    int insert(WarehouseReceipt receipt);
    WarehouseReceipt getById(int id);
    List<WarehouseReceipt> getAll();
    WarehouseReceipt getByOrderId(int orderId);
}
