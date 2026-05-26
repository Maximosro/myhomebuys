package com.sro.myhomebuys.web;

import com.sro.myhomebuys.receipts.controller.dto.ReceiptListResponse;
import com.sro.myhomebuys.receipts.controller.dto.ReceiptResponse;
import com.sro.myhomebuys.receipts.model.Store;
import com.sro.myhomebuys.receipts.service.ReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class WebController {

  private final ReceiptService receiptService;

  @GetMapping
  public String home(Model model) {
    model.addAttribute("activeModule", "home");
    return "home";
  }

  @GetMapping("/receipts")
  public String list(Model model,
      @RequestParam(required = false) String store,
      @RequestParam(value = "total-min", required = false) BigDecimal totalMin,
      @RequestParam(value = "total-max", required = false) BigDecimal totalMax,
      @RequestParam(value = "date-before", required = false) LocalDate dateBefore,
      @RequestParam(value = "date-after", required = false) LocalDate dateAfter) {

    boolean hasParams = store != null || totalMin != null || totalMax != null
        || dateBefore != null || dateAfter != null;

    List<ReceiptListResponse> receipts;
    if (hasParams) {
      receipts = receiptService.search(store, totalMin, totalMax, dateAfter, dateBefore);
    } else {
      receipts = receiptService.listAll();
    }

    model.addAttribute("activeModule", "receipts");
    model.addAttribute("receipts", receipts);
    model.addAttribute("store", store);
    model.addAttribute("totalMin", totalMin);
    model.addAttribute("totalMax", totalMax);
    model.addAttribute("dateBefore", dateBefore);
    model.addAttribute("dateAfter", dateAfter);
    return "receipts/list";
  }

  @GetMapping("/receipts/list")
  public String listFragment(Model model,
      @RequestParam(required = false) String store,
      @RequestParam(value = "total-min", required = false) BigDecimal totalMin,
      @RequestParam(value = "total-max", required = false) BigDecimal totalMax,
      @RequestParam(value = "date-before", required = false) LocalDate dateBefore,
      @RequestParam(value = "date-after", required = false) LocalDate dateAfter) {

    boolean hasParams = store != null || totalMin != null || totalMax != null
        || dateBefore != null || dateAfter != null;

    List<ReceiptListResponse> receipts;
    if (hasParams) {
      receipts = receiptService.search(store, totalMin, totalMax, dateAfter, dateBefore);
    } else {
      receipts = receiptService.listAll();
    }

    model.addAttribute("receipts", receipts);
    return "receipts/fragments :: table";
  }

  @GetMapping("/receipts/upload")
  public String uploadForm(Model model) {
    model.addAttribute("activeModule", "receipts");
    model.addAttribute("stores", Store.values());
    return "receipts/upload";
  }

  @PostMapping(value = "/receipts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public String upload(@RequestParam("store") String store,
      @RequestParam("file") MultipartFile file,
      RedirectAttributes redirectAttributes) {

    ReceiptResponse receipt = receiptService.uploadReceipt(store, file);
    redirectAttributes.addFlashAttribute("successMessage",
        "Receipt uploaded: " + receipt.getStore() + " — " + receipt.getDate());
    return "redirect:/receipts";
  }

  @GetMapping("/receipts/{id}")
  public String detail(@PathVariable UUID id, Model model) {
    ReceiptResponse receipt = receiptService.getById(id);
    model.addAttribute("activeModule", "receipts");
    model.addAttribute("receipt", receipt);
    return "receipts/detail";
  }

  @DeleteMapping("/receipts/{id}")
  public String deleteOne(@PathVariable UUID id) {
    receiptService.delete(id);
    return "receipts/fragments :: empty";
  }

  @PostMapping("/receipts/{id}/delete")
  public String deleteOneFromPage(@PathVariable UUID id) {
    receiptService.delete(id);
    return "redirect:/receipts";
  }

  @PostMapping("/receipts/delete-all")
  public String deleteAllFromPage() {
    receiptService.deleteAll();
    return "redirect:/receipts";
  }
}
