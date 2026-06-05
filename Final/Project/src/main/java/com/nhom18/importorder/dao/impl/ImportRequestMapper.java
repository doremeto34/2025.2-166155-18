package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ImportRequestMapper {

    public static ImportRequest mapRequest(ResultSet rs) throws SQLException {
        ImportRequest req = new ImportRequest(
            rs.getInt("id"),
            rs.getInt("created_by"),
            LocalDate.parse(rs.getString("created_date")),
            RequestStatus.valueOf(rs.getString("status"))
        );
        req.setCreatorName(rs.getString("creator_name"));
        return req;
    }

    public static ImportRequestItem mapRequestItem(ResultSet rs) throws SQLException {
        ImportRequestItem item = new ImportRequestItem(
            rs.getInt("id"),
            rs.getInt("request_id"),
            rs.getString("merchandise_code"),
            rs.getInt("quantity_ordered"),
            rs.getInt("quantity_shortage"),
            rs.getString("unit"),
            LocalDate.parse(rs.getString("desired_delivery_date"))
        );
        item.setMerchandiseName(rs.getString("merchandise_name"));
        return item;
    }
}
