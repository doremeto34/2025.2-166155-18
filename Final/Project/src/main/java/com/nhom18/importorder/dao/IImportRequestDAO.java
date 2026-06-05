package com.nhom18.importorder.dao;

import com.nhom18.importorder.model.entity.ImportRequest;
import com.nhom18.importorder.model.entity.ImportRequestItem;
import com.nhom18.importorder.model.enums.RequestStatus;
import java.util.List;

public interface IImportRequestDAO {
    int insert(ImportRequest request);
    void insertItem(ImportRequestItem item);
    List<ImportRequest> getAllWithCreatorName();
    ImportRequest getById(int id);
    void updateStatus(int requestId, RequestStatus status);
    void adjustShortageQuantity(int itemId, int delta);
    List<ImportRequestItem> getPendingRequestItems();
}
