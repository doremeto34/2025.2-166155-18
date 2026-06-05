package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.SiteInventory;
import java.util.List;

public interface ISiteInventoryDAO {
    List<SiteInventory> getByMerchandiseCode(String merchandiseCode);
    List<SiteInventory> getBySiteCode(String siteCode);
    SiteInventory get(String siteCode, String merchandiseCode);
    void updateStock(String siteCode, String merchandiseCode, int newQuantity);
}
