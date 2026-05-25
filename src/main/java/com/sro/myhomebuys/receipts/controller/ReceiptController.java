package com.sro.myhomebuys.receipts.controller;

import com.sro.myhomebuys.receipts.controller.dto.ReceiptListResponse;
import com.sro.myhomebuys.receipts.controller.dto.ReceiptResponse;
import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.model.Store;
import com.sro.myhomebuys.receipts.service.ReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @GetMapping("/search")
  public ResponseEntity<List<ReceiptListResponse>> search(@RequestParam(required = false) String store,
      @RequestParam(value = "total-min", required = false) BigDecimal totalMin,
      @RequestParam(value = "total-max", required = false) BigDecimal totalMax,
      @RequestParam(value = "date-before", required = false) LocalDate dateB,
      @RequestParam(value = "date-after", required = false) LocalDate dateA) {
    List<ReceiptListResponse> result = receiptService.search(store, totalMin, totalMax, dateA, dateB);
    return ResponseEntity.ok(result);
  }

  @GetMapping
  public ResponseEntity<List<ReceiptListResponse>> listAll() {
    return ResponseEntity.ok(receiptService.listAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReceiptResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(receiptService.getById(id));
  }

  @DeleteMapping("/{id}")
  public void deleteById(@PathVariable UUID id) {
    receiptService.delete(id);
  }

  @DeleteMapping
  public void deleteAll() {
    receiptService.deleteAll();
  }
}
