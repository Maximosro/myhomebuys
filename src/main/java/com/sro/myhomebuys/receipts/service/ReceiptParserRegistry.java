package com.sro.myhomebuys.receipts.service;

import com.sro.myhomebuys.receipts.exception.UnsupportedStoreException;
import com.sro.myhomebuys.receipts.parser.ReceiptParser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceiptParserRegistry {

    private final List<ReceiptParser> parsers;

    public ReceiptParserRegistry(List<ReceiptParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public ReceiptParser findParser(String store) {
        return parsers.stream()
                .filter(p -> p.canHandle(store))
                .findFirst()
                .orElseThrow(() -> new UnsupportedStoreException(store));
    }
}
