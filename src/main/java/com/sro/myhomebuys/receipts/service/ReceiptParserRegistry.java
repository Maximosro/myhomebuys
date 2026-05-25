package com.sro.myhomebuys.receipts.service;

import com.sro.myhomebuys.receipts.exception.UnsupportedStoreException;
import com.sro.myhomebuys.receipts.parser.ReceiptParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ReceiptParserRegistry {

    private final List<ReceiptParser> parsers;

    public ReceiptParserRegistry(List<ReceiptParser> parsers) {
        this.parsers = List.copyOf(parsers);
        log.info("Registered {} receipt parsers", parsers.size());
    }

    public ReceiptParser findParser(String store) {
        log.debug("Looking up parser for store: {}", store);
        return parsers.stream()
                .filter(p -> p.canHandle(store))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No parser found for store: {}", store);
                    return new UnsupportedStoreException(store);
                });
    }
}
