package com.sro.myhomebuys.receipts.parser;

import com.sro.myhomebuys.receipts.model.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedReceipt {

    private Store store;
    private LocalDate date;
    private BigDecimal total;
    private List<ParsedItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParsedItem {
        private String productName;
        private Integer quantity;
        private BigDecimal pricePerUnit;
    }
}
