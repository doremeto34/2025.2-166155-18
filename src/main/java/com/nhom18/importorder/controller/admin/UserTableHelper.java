package com.nhom18.importorder.controller.admin;

import com.nhom18.importorder.model.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class UserTableHelper {

    public static void setupColumns(
            TableColumn<User, Integer> colId,
            TableColumn<User, String> colUsername,
            TableColumn<User, String> colFullName,
            TableColumn<User, String> colRole,
            TableColumn<User, String> colSiteCode,
            TableColumn<User, String> colStatus) {

        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colFullName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole().name()));
        colSiteCode.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getSiteCode() != null ? cell.getValue().getSiteCode() : "-"
        ));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().isActive() ? "Đang hoạt động" : "Ngừng hoạt động"
        ));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    getStyleClass().removeAll("badge", "badge-success", "badge-rejected");
                } else {
                    setText(item);
                    getStyleClass().add("badge");
                    if (item.equals("Đang hoạt động")) {
                        getStyleClass().removeAll("badge-rejected");
                        getStyleClass().add("badge-success");
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else {
                        getStyleClass().removeAll("badge-success");
                        getStyleClass().add("badge-rejected");
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    public static List<User> filter(List<User> allUsers, String keyword, String roleFilter, String statusFilter) {
        if (allUsers == null) return List.of();
        String kw = keyword.toLowerCase().trim();
        return allUsers.stream()
            .filter(user -> {
                boolean matchesKeyword = kw.isEmpty() ||
                    user.getUsername().toLowerCase().contains(kw) ||
                    user.getFullName().toLowerCase().contains(kw);
                boolean matchesRole = "Tất cả".equals(roleFilter) || user.getRole().name().equals(roleFilter);
                boolean matchesStatus = true;
                if ("Đang hoạt động".equals(statusFilter)) matchesStatus = user.isActive();
                else if ("Ngừng hoạt động".equals(statusFilter)) matchesStatus = !user.isActive();
                return matchesKeyword && matchesRole && matchesStatus;
            })
            .collect(Collectors.toList());
    }
}
