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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ReceiptController {

  private final ReceiptService receiptService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ReceiptResponse> upload(
      @RequestParam("store") String store,
      @RequestParam("file") MultipartFile file) {

    log.info("Uploading receipt for store: {}", store);
    if (file.isEmpty()) {
      log.warn("Uploaded file is empty");
      throw new ReceiptParsingException("Uploaded file is empty");
    }
    ReceiptResponse response = receiptService.uploadReceipt(store, file);
    log.info("Receipt uploaded successfully: {}", response.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/search")
  public ResponseEntity<List<ReceiptListResponse>> search(@RequestParam(required = false) String store,
      @RequestParam(value = "total-min", required = false) BigDecimal totalMin,
      @RequestParam(value = "total-max", required = false) BigDecimal totalMax,
      @RequestParam(value = "date-before", required = false) LocalDate dateB,
      @RequestParam(value = "date-after", required = false) LocalDate dateA) {
    log.debug("Searching receipts: store={}, totalMin={}, totalMax={}, dateA={}, dateB={}",
        store, totalMin, totalMax, dateA, dateB);
    List<ReceiptListResponse> result = receiptService.search(store, totalMin, totalMax, dateA, dateB);
    return ResponseEntity.ok(result);
  }

  @GetMapping
  public ResponseEntity<List<ReceiptListResponse>> listAll() {
    log.info("Listing all receipts");
    return ResponseEntity.ok(receiptService.listAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReceiptResponse> getById(@PathVariable UUID id) {
    log.info("Fetching receipt: {}", id);
    return ResponseEntity.ok(receiptService.getById(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
    log.info("Deleting receipt: {}", id);
    receiptService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> deleteAll() {
    log.warn("Deleting all receipts");
    receiptService.deleteAll();
    return ResponseEntity.noContent().build();
  }
}
