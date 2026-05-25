package com.sro.myhomebuys.receipts.parser;

import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.model.Store;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MercadonaOnlineParser implements ReceiptParser {

    private static final String STORE_NAME = "MERCADONA";
    private static final Pattern DATE_PATTERN = Pattern.compile("Cobrado el (\\d{2})/(\\d{2})/(\\d{2})");
    private static final Pattern PRODUCT_LINE = Pattern.compile("^(.+?)\\s+(\\d+)\\s+(\\d{1,3}[.,]\\d{2})\\s*€$");
    private static final Pattern PRICE_ONLY = Pattern.compile("(\\d{1,3}[.,]\\d{2})\\s*€?$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");

    private static final String HEADER_LINE = "Nombre Producto";
    private static final String SUBTOTAL_PREFIX = "Productos ";
    private static final String PREP_PREFIX = "Coste de preparaci";
    private static final String TOTAL_PREFIX = "TOTAL ";
    private static final String TAX_HEADER = "Desglose de impuestos";
    private static final String TAX_COLUMNS = "IVA Base Cuota";
    private static final String LEGAL_MARKER = "MERCADONA.S.A";

    @Override
    public boolean canHandle(String store) {
        return STORE_NAME.equalsIgnoreCase(store);
    }

    @Override
    public ParsedReceipt parse(InputStream pdfStream) throws ReceiptParsingException {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return parseText(text);
        } catch (IOException e) {
            throw new ReceiptParsingException("Failed to read PDF file", e);
        }
    }

    ParsedReceipt parseText(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }

        LocalDate date = extractDate(lines);
        List<ParsedReceipt.ParsedItem> items = extractItems(lines);
        BigDecimal total = extractTotal(lines);

        if (items.isEmpty()) {
            throw new ReceiptParsingException("No products found in the PDF");
        }
        if (total == null) {
            throw new ReceiptParsingException("Total not found in the PDF");
        }

        return ParsedReceipt.builder()
                .store(Store.MERCADONA)
                .date(date)
                .total(total)
                .items(items)
                .build();
    }

    private LocalDate extractDate(List<String> lines) {
        for (String line : lines) {
            Matcher m = DATE_PATTERN.matcher(line);
            if (m.find()) {
                return LocalDate.parse(m.group(1) + "/" + m.group(2) + "/" + m.group(3), DATE_FORMAT);
            }
        }
        throw new ReceiptParsingException("Cobrado date not found in PDF");
    }

    private List<ParsedReceipt.ParsedItem> extractItems(List<String> lines) {
        List<ParsedReceipt.ParsedItem> items = new ArrayList<>();

        for (String line : lines) {
            // Skip non-product lines
            if (isNonProductLine(line)) {
                continue;
            }

            Matcher m = PRODUCT_LINE.matcher(line);
            if (m.find()) {
                String name = m.group(1);
                int quantity = Integer.parseInt(m.group(2));
                BigDecimal price = new BigDecimal(m.group(3).replace(",", "."));

                items.add(ParsedReceipt.ParsedItem.builder()
                        .productName(name)
                        .quantity(quantity)
                        .pricePerUnit(price)
                        .build());
            }
        }

        return items;
    }

    private boolean isNonProductLine(String line) {
        return line.startsWith(HEADER_LINE)
                || line.startsWith(SUBTOTAL_PREFIX)
                || line.startsWith(PREP_PREFIX)
                || line.startsWith(TOTAL_PREFIX)
                || line.startsWith(TAX_HEADER)
                || line.startsWith(TAX_COLUMNS)
                || line.contains(LEGAL_MARKER)
                || line.startsWith("Total ")
                || line.contains("%")
                || (line.startsWith("Pedido ") && line.contains("Nº"));
    }

    private BigDecimal extractTotal(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith(TOTAL_PREFIX)) {
                Matcher m = PRICE_ONLY.matcher(line);
                if (m.find()) {
                    return new BigDecimal(m.group(1).replace(",", "."));
                }
            }
        }
        return null;
    }
}
