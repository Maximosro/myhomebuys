package com.sro.myhomebuys.receipts.controller.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class MonthTotal {

  private String month;
  private BigDecimal total;
}
