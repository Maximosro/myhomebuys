package com.sro.myhomebuys.receipts.controller.dto;

import com.sro.myhomebuys.receipts.model.Receipt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ReceiptListResponse {
    private UUID id;
    private String store;
    private LocalDate date;
    private BigDecimal total;

    public static ReceiptListResponse from(Receipt receipt) {
        return ReceiptListResponse.builder()
                .id(receipt.getId())
                .store(receipt.getStore().name())
                .date(receipt.getDate())
                .total(receipt.getTotal())
                .build();
    }
}
