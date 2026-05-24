package com.sro.myhomebuys.receipts.repository;

import com.sro.myhomebuys.receipts.model.MercadonaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MercadonaItemRepository extends JpaRepository<MercadonaItem, Long> {

    List<MercadonaItem> findByReceiptId(UUID receiptId);
}
