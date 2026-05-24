package com.sro.myhomebuys.receipts.exception;

public class UnsupportedStoreException extends RuntimeException {

    public UnsupportedStoreException(String store) {
        super("No parser found for store: " + store);
    }
}
