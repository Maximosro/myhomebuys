package com.sro.myhomebuys.receipts.repository;

import com.sro.myhomebuys.receipts.model.Receipt;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

  List<Receipt> findAllByOrderByDateDesc();

  @Query("""
      SELECT r FROM Receipt r WHERE
      (:store IS NULL OR LOWER(r.store) LIKE LOWER(CONCAT('%', :store, '%'))) AND
      (:totalMin IS NULL OR r.total >= :totalMin) AND
      (:totalMax IS NULL OR r.total <= :totalMax) AND
      (:dateA IS NULL OR r.date >= :dateA) AND
      (:dateB IS NULL OR r.date <= :dateB)
      """)
  List<Receipt> search(@Param("store") String store, @Param("totalMin") BigDecimal totalMin,
      @Param("totalMax") BigDecimal totalMax, @Param("dateA") LocalDate dateA,
      @Param("dateB") LocalDate dateB);

}
