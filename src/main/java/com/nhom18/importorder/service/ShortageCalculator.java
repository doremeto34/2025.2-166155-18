package com.nhom18.importorder.service;

import com.nhom18.importorder.dao.ICompanyInventoryDAO;
import com.nhom18.importorder.model.entity.CompanyInventory;
import com.nhom18.importorder.model.entity.ImportRequestItem;

public class ShortageCalculator {

    public static int deductInventoryAndGetShortage(ICompanyInventoryDAO companyInventoryDAO, ImportRequestItem item) {
        CompanyInventory stock = companyInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
        int orderedQty = item.getQuantityOrdered();
        int shortage = orderedQty;
        
        if (stock != null && stock.getInStockQuantity() > 0) {
            int available = stock.getInStockQuantity();
            int fulfilled = Math.min(orderedQty, available);
            shortage = orderedQty - fulfilled;
            companyInventoryDAO.updateStock(item.getMerchandiseCode(), available - fulfilled);
            System.out.println("MRP: Đã tự động đáp ứng " + fulfilled + " " + item.getUnit() + 
                               " của SP " + item.getMerchandiseCode() + " từ kho nội bộ. Lượng tồn mới: " + (available - fulfilled));
        }
        return shortage;
    }

    public static void rollbackInventory(ICompanyInventoryDAO companyInventoryDAO, ImportRequestItem item) {
        int fulfilledQty = item.getQuantityOrdered() - item.getQuantityShortage();
        if (fulfilledQty > 0) {
            CompanyInventory stock = companyInventoryDAO.getByMerchandiseCode(item.getMerchandiseCode());
            if (stock != null) {
                companyInventoryDAO.updateStock(item.getMerchandiseCode(), stock.getInStockQuantity() + fulfilledQty);
                System.out.println("MRP Rollback: Đã hoàn trả lại " + fulfilledQty + " " + item.getUnit() + 
                                   " của SP " + item.getMerchandiseCode() + " vào kho nội bộ.");
            }
        }
    }
}
