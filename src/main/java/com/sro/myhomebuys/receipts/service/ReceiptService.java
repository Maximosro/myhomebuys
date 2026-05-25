package com.sro.myhomebuys.receipts.service;

import com.sro.myhomebuys.receipts.controller.dto.ReceiptListResponse;
import com.sro.myhomebuys.receipts.controller.dto.ReceiptResponse;
import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.model.MercadonaItem;
import com.sro.myhomebuys.receipts.model.Receipt;
import com.sro.myhomebuys.receipts.model.Store;
import com.sro.myhomebuys.receipts.parser.ParsedReceipt;
import com.sro.myhomebuys.receipts.parser.ReceiptParser;
import com.sro.myhomebuys.receipts.repository.MercadonaItemRepository;
import com.sro.myhomebuys.receipts.repository.ReceiptRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

  private final ReceiptParserRegistry parserRegistry;
  private final ReceiptRepository receiptRepository;
  private final MercadonaItemRepository mercadonaItemRepository;

  public ReceiptResponse uploadReceipt(String store, MultipartFile file) {
    ReceiptParser parser = parserRegistry.findParser(store);

    try (InputStream is = file.getInputStream()) {
      ParsedReceipt parsed = parser.parse(is);
      Receipt receipt = saveReceipt(parsed);
      return ReceiptResponse.from(receipt, parsed.getItems());
    } catch (IOException e) {
      throw new ReceiptParsingException("Failed to read uploaded file", e);
    }
  }

  @Transactional(readOnly = true)
  public List<ReceiptListResponse> search(String store, BigDecimal totalMin, BigDecimal totalMax,
      LocalDate dateAfter, LocalDate dateBefore) {
    if (totalMin != null && totalMax != null && totalMin.compareTo(totalMax) > 0) {
      log.error("El Total minimo debe ser menor o igual al total maximo");
      return List.of();
    }

    if (dateAfter != null && dateBefore != null && dateAfter.isAfter(dateBefore)) {
      log.error("La fecha de inicio debe ser menor o igual a la fecha final");
      return List.of();
    }

    return receiptRepository.search(store, totalMin, totalMax, dateAfter, dateBefore).stream()
        .map(ReceiptListResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<ReceiptListResponse> listAll() {
    return receiptRepository.findAllByOrderByDateDesc().stream()
        .map(ReceiptListResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ReceiptResponse getById(UUID id) {
    Receipt receipt = receiptRepository.findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found: " + id));
    List<ParsedReceipt.ParsedItem> items = loadItems(receipt);
    return ReceiptResponse.from(receipt, items);
  }

  @Transactional
  public void delete(UUID id) {
    receiptRepository.deleteById(id);
    mercadonaItemRepository.deleteAllByIdInBatch(
        mercadonaItemRepository.findByReceiptId(id).stream()
            .map(MercadonaItem::getId)
            .toList());
  }

  @Transactional
  public boolean deleteAll() {
    receiptRepository.deleteAll();
    mercadonaItemRepository.deleteAll();
    return true;
  }

  private Receipt saveReceipt(ParsedReceipt parsed) {
    Receipt receipt = Receipt.builder()
        .store(parsed.getStore())
        .date(parsed.getDate())
        .total(parsed.getTotal())
        .build();
    receipt = receiptRepository.save(receipt);

    List<MercadonaItem> items = toMercadonaItems(receipt, parsed.getItems());
    mercadonaItemRepository.saveAll(items);

    return receipt;
  }

  private List<ParsedReceipt.ParsedItem> loadItems(Receipt receipt) {
    if (receipt.getStore() == Store.MERCADONA) {
      return mercadonaItemRepository.findByReceiptId(receipt.getId()).stream()
          .map(item -> ParsedReceipt.ParsedItem.builder()
              .productName(item.getProductName())
              .quantity(item.getQuantity())
              .pricePerUnit(item.getPricePerUnit())
              .build())
          .toList();
    }
    return List.of();
  }

  private List<MercadonaItem> toMercadonaItems(Receipt receipt,
      List<ParsedReceipt.ParsedItem> items) {
    return items.stream()
        .map(i -> MercadonaItem.builder()
            .receipt(receipt)
            .productName(i.getProductName())
            .quantity(i.getQuantity())
            .pricePerUnit(i.getPricePerUnit())
            .build())
        .toList();
  }
}
