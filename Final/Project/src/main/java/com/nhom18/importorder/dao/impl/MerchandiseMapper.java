package com.nhom18.importorder.dao.impl;

import com.nhom18.importorder.model.entity.Merchandise;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MerchandiseMapper {

    public static Merchandise mapMerchandise(ResultSet rs) throws SQLException {
        return new Merchandise(
            rs.getString("merchandise_code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("unit"),
            rs.getDouble("price"),
            rs.getInt("active") == 1
        );
    }
}
