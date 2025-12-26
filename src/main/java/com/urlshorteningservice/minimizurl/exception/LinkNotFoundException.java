package com.urlshorteningservice.minimizurl.exception;

public class LinkNotFoundException extends MinimizUrlException {
    public LinkNotFoundException(String shortCode) {
        super("Link with code '" + shortCode + "' was not found.");
    }
}
