package com.sro.myhomebuys.receipts.repository;

import com.sro.myhomebuys.receipts.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    List<Receipt> findAllByOrderByDateDesc();
}
