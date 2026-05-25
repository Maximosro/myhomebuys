package com.sro.myhomebuys.receipts.exception;

public class ReceiptParsingException extends RuntimeException {

    public ReceiptParsingException(String message) {
        super(message);
    }

    public ReceiptParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
