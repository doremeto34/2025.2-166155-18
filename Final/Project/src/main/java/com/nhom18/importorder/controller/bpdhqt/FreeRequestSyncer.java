package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class FreeRequestSyncer {

    public static void sync(CreateFreeRequestController controller) {
        Map<String, Integer> totalQuantities = new HashMap<>();
        Map<String, String> merchNames = new HashMap<>();
        Map<String, String> merchUnits = new HashMap<>();

        for (Order order : controller.getProposedOrdersData()) {
            for (OrderItem oi : order.getItems()) {
                String code = oi.getMerchandiseCode();
                totalQuantities.put(code, totalQuantities.getOrDefault(code, 0) + oi.getQuantityOrdered());
                merchNames.put(code, oi.getMerchandiseName());
                merchUnits.put(code, oi.getUnit());
            }
        }

        controller.getSelectedItemsData().removeIf(item -> !totalQuantities.containsKey(item.getMerchandiseCode()));

        for (Map.Entry<String, Integer> entry : totalQuantities.entrySet()) {
            String code = entry.getKey();
            int qty = entry.getValue();

            boolean exists = false;
            for (ImportRequestItem item : controller.getSelectedItemsData()) {
                if (item.getMerchandiseCode().equals(code)) {
                    item.setQuantityOrdered(qty);
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                ImportRequestItem newItem = new ImportRequestItem();
                newItem.setMerchandiseCode(code);
                newItem.setMerchandiseName(merchNames.get(code));
                newItem.setQuantityOrdered(qty);
                newItem.setUnit(merchUnits.get(code));
                newItem.setDesiredDeliveryDate(controller.getDpRequiredDate().getValue() != null ? controller.getDpRequiredDate().getValue() : LocalDate.now().plusDays(10));
                controller.getSelectedItemsData().add(newItem);
            }
        }

        controller.getTblSelectedItems().refresh();
        controller.updateTotalQuantityLabel();
    }
}
