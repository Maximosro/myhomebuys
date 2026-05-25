package com.sro.myhomebuys.receipts.controller.dto;

import com.sro.myhomebuys.receipts.model.Receipt;
import com.sro.myhomebuys.receipts.parser.ParsedReceipt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ReceiptResponse {
    private UUID id;
    private String store;
    private LocalDate date;
    private BigDecimal total;
    private List<ItemResponse> items;

    @Data
    @AllArgsConstructor
    @Builder
    public static class ItemResponse {
        private String productName;
        private Integer quantity;
        private BigDecimal pricePerUnit;
    }

    public static ReceiptResponse from(Receipt receipt, List<ParsedReceipt.ParsedItem> items) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .store(receipt.getStore().name())
                .date(receipt.getDate())
                .total(receipt.getTotal())
                .items(items.stream()
                        .map(i -> ItemResponse.builder()
                                .productName(i.getProductName())
                                .quantity(i.getQuantity())
                                .pricePerUnit(i.getPricePerUnit())
                                .build())
                        .toList())
                .build();
    }
}
