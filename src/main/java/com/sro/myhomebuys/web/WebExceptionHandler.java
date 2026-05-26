package com.sro.myhomebuys.web;

import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.exception.UnsupportedStoreException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice("com.sro.myhomebuys.web")
@Slf4j
public class WebExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public String handleResponseStatus(ResponseStatusException ex, Model model,
      HttpServletRequest request, HttpServletResponse response) {
    log.warn("Web error: {} — {}", ex.getStatusCode(), ex.getReason());
    response.setStatus(ex.getStatusCode().value());
    model.addAttribute("status", ex.getStatusCode().value());
    model.addAttribute("message", ex.getReason());
    return resolveView(request, model);
  }

  @ExceptionHandler({ReceiptParsingException.class, UnsupportedStoreException.class,
      IllegalArgumentException.class})
  public String handleBadRequest(RuntimeException ex, Model model,
      HttpServletRequest request, HttpServletResponse response) {
    log.warn("Web bad request: {}", ex.getMessage());
    response.setStatus(400);
    model.addAttribute("status", 400);
    model.addAttribute("message", ex.getMessage());
    return resolveView(request, model);
  }

  @ExceptionHandler(Exception.class)
  public String handleGeneral(Exception ex, Model model, HttpServletRequest request,
      HttpServletResponse response) {
    log.error("Web unexpected error", ex);
    response.setStatus(500);
    model.addAttribute("status", 500);
    model.addAttribute("message", "Internal server error");
    return resolveView(request, model);
  }

  private String resolveView(HttpServletRequest request, Model model) {
    if ("true".equals(request.getHeader("HX-Request"))) {
      return "fragments/error-toast :: toast";
    }
    return "error";
  }
}
