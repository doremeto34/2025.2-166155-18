package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.CompanyInventory;
import java.util.List;

public interface ICompanyInventoryDAO {
    List<CompanyInventory> getAll();
    CompanyInventory getByMerchandiseCode(String merchandiseCode);
    void updateStock(String merchandiseCode, int newQuantity);
}
