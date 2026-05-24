package com.sro.myhomebuys.receipts.parser;

import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;

import java.io.InputStream;

public interface ReceiptParser {

    boolean canHandle(String store);

    ParsedReceipt parse(InputStream pdfStream) throws ReceiptParsingException;
}
