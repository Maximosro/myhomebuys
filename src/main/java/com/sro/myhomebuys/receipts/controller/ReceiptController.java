package com.sro.myhomebuys.receipts.controller;

import com.sro.myhomebuys.receipts.controller.dto.ReceiptListResponse;
import com.sro.myhomebuys.receipts.controller.dto.ReceiptResponse;
import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptResponse> upload(
            @RequestParam("store") String store,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ReceiptParsingException("Uploaded file is empty");
        }
        ReceiptResponse response = receiptService.uploadReceipt(store, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReceiptListResponse>> listAll() {
        return ResponseEntity.ok(receiptService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(receiptService.getById(id));
    }
}
