package com.nhom18.importorder.controller.bpdhqt;

import com.nhom18.importorder.model.entity.Order;
import com.nhom18.importorder.model.entity.OrderItem;
import com.nhom18.importorder.model.entity.Site;
import com.nhom18.importorder.model.enums.DeliveryMethod;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.time.LocalDate;

public class OrderCardFormSetup {

    public static void setup(CreateFreeRequestController controller, Order order, VBox card, VBox vboxItems) {
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(10);

        Label lblSite = new Label("Site đối tác:");
        lblSite.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 12px;");
        formGrid.add(lblSite, 0, 0);

        ComboBox<Site> cbSite = new ComboBox<>(FXCollections.observableArrayList(controller.getActiveSites()));
        cbSite.setPrefWidth(280.0);
        cbSite.setConverter(new StringConverter<Site>() {
            @Override public String toString(Site s) { return s == null ? "" : s.getName() + " (" + s.getSiteCode() + ")"; }
            @Override public Site fromString(String string) { return null; }
        });
        controller.getActiveSites().stream()
                .filter(s -> s.getSiteCode().equals(order.getSiteCode()))
                .findFirst()
                .ifPresent(cbSite::setValue);
        formGrid.add(cbSite, 1, 0);

        Label lblMethod = new Label("Vận chuyển:");
        lblMethod.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-font-size: 12px;");
        formGrid.add(lblMethod, 0, 1);

        ComboBox<String> cbMethod = new ComboBox<>();
        cbMethod.setPrefWidth(280.0);
        formGrid.add(cbMethod, 1, 1);
        card.getChildren().add(formGrid);

        Label lblSummary = new Label();
        lblSummary.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
        card.getChildren().add(lblSummary);

        Runnable updateOrderMetrics = () -> updateMetrics(controller, order, cbSite.getValue(), cbMethod, lblSummary);

        cbSite.valueProperty().addListener((obs, oldSite, newSite) -> {
            if (newSite != null) {
                updateOrderMetrics.run();
                if (oldSite != null) controller.resetCheckPassed();
            }
        });

        cbMethod.valueProperty().addListener((obs, oldMethod, newMethod) -> {
            if (newMethod != null) {
                order.setDeliveryMethod(newMethod.contains("SHIP") ? DeliveryMethod.SHIP : DeliveryMethod.AIR);
                updateOrderMetrics.run();
                if (oldMethod != null) controller.resetCheckPassed();
            }
        });

        updateOrderMetrics.run();
        OrderCardItemRenderer.render(controller, order, vboxItems, updateOrderMetrics);
        card.getChildren().add(vboxItems);
    }

    private static void updateMetrics(CreateFreeRequestController controller, Order order, Site site, ComboBox<String> cbMethod, Label lblSummary) {
        if (site != null) {
            order.setSiteCode(site.getSiteCode());
            order.setSiteName(site.getName());

            cbMethod.setItems(FXCollections.observableArrayList(
                "Đường biển (SHIP - " + site.getShipDays() + " ngày)",
                "Đường hàng không (AIR - " + site.getAirDays() + " ngày)"
            ));

            if (order.getDeliveryMethod() == DeliveryMethod.SHIP) {
                cbMethod.setValue("Đường biển (SHIP - " + site.getShipDays() + " ngày)");
                order.setEstimatedArrival(LocalDate.now().plusDays(site.getShipDays()));
            } else {
                cbMethod.setValue("Đường hàng không (AIR - " + site.getAirDays() + " ngày)");
                order.setEstimatedArrival(LocalDate.now().plusDays(site.getAirDays()));
            }
        }

        double totalVal = 0;
        int itemsCount = 0;
        for (OrderItem oi : order.getItems()) {
            double pr = controller.getMerchandisePrices().getOrDefault(oi.getMerchandiseCode(), 0.0);
            totalVal += pr * oi.getQuantityOrdered();
            itemsCount += oi.getQuantityOrdered();
        }

        int days = (order.getDeliveryMethod() == DeliveryMethod.SHIP && site != null) ? site.getShipDays() : (site != null ? site.getAirDays() : 0);
        lblSummary.setText(String.format("Tổng tiền: %,.2f USD  •  %d sản phẩm  •  Giao hàng: %d ngày (Dự kiến đến: %s)", 
            totalVal, itemsCount, days, order.getEstimatedArrival().toString()));
    }
}
