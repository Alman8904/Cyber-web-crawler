package com.supply.supplyTrace.crawler;

public class SiteCrawlException extends RuntimeException {

    public SiteCrawlException(String message) {
        super(message);
    }

    public SiteCrawlException(String message, Throwable cause) {
        super(message, cause);
    }
}

