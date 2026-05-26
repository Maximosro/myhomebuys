package com.sro.myhomebuys.receipts.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DashboardData {

  private BigDecimal totalSpent;
  private long receiptCount;
  private BigDecimal averageTotal;
  private List<MonthTotal> monthlyTotals;
  private List<ReceiptListResponse> recentReceipts;
}
