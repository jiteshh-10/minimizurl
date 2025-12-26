package com.urlshorteningservice.minimizurl.exception;

// We extend RuntimeException so it's an "unchecked" exception
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("The username '" + username + "' is already taken.");
    }
}