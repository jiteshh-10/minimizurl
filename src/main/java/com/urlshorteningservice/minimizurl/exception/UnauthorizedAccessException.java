package com.urlshorteningservice.minimizurl.exception;

public class UnauthorizedAccessException extends MinimizUrlException {
    public UnauthorizedAccessException() {
        super("You do not have permission to perform this action.");
    }
}
