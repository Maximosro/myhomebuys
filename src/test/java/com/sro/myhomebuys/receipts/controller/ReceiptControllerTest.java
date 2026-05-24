package com.sro.myhomebuys.receipts.controller;

import com.sro.myhomebuys.receipts.controller.dto.ReceiptListResponse;
import com.sro.myhomebuys.receipts.controller.dto.ReceiptResponse;
import com.sro.myhomebuys.receipts.exception.GlobalExceptionHandler;
import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.exception.UnsupportedStoreException;
import com.sro.myhomebuys.receipts.service.ReceiptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiptController.class)
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiptService receiptService;

    @Test
    @DisplayName("POST /api/receipts/upload: returns 201 with receipt")
    void uploadValidPdf() throws Exception {
        UUID id = UUID.randomUUID();
        ReceiptResponse response = ReceiptResponse.builder()
                .id(id)
                .store("MERCADONA")
                .date(LocalDate.of(2026, 5, 7))
                .total(new BigDecimal("146.43"))
                .items(List.of())
                .build();

        when(receiptService.uploadReceipt(eq("MERCADONA"), any()))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "fake pdf".getBytes());

        mockMvc.perform(multipart("/api/receipts/upload")
                        .file(file)
                        .param("store", "MERCADONA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.store").value("MERCADONA"))
                .andExpect(jsonPath("$.total").value(146.43));
    }

    @Test
    @DisplayName("POST /api/receipts/upload: returns 400 for empty file")
    void uploadEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/receipts/upload")
                        .file(file)
                        .param("store", "MERCADONA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARSE_ERROR"));
    }

    @Test
    @DisplayName("POST /api/receipts/upload: returns 400 for unsupported store")
    void uploadUnsupportedStore() throws Exception {
        when(receiptService.uploadReceipt(eq("LIDL"), any()))
                .thenThrow(new UnsupportedStoreException("LIDL"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "fake pdf".getBytes());

        mockMvc.perform(multipart("/api/receipts/upload")
                        .file(file)
                        .param("store", "LIDL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_STORE"));
    }

    @Test
    @DisplayName("GET /api/receipts: returns list")
    void listAll() throws Exception {
        UUID id = UUID.randomUUID();
        when(receiptService.listAll()).thenReturn(List.of(
                ReceiptListResponse.builder()
                        .id(id)
                        .store("MERCADONA")
                        .date(LocalDate.of(2026, 5, 7))
                        .total(new BigDecimal("146.43"))
                        .build()
        ));

        mockMvc.perform(get("/api/receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].store").value("MERCADONA"));
    }

    @Test
    @DisplayName("GET /api/receipts/{id}: returns receipt with items")
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        ReceiptResponse response = ReceiptResponse.builder()
                .id(id)
                .store("MERCADONA")
                .date(LocalDate.of(2026, 5, 7))
                .total(new BigDecimal("146.43"))
                .items(List.of(
                        ReceiptResponse.ItemResponse.builder()
                                .productName("Leche")
                                .quantity(2)
                                .pricePerUnit(new BigDecimal("1.15"))
                                .build()
                ))
                .build();

        when(receiptService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/receipts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Leche"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }
}
