package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.Site;
import java.util.List;

public interface ISiteDAO {
    Site getByCode(String siteCode);
    List<Site> getAllActive();
    List<Site> getAll();
    void insert(Site site);
    void update(Site site);
}
