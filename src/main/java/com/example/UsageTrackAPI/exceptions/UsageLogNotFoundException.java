package com.example.UsageTrackAPI.exceptions;

public class UsageLogNotFoundException extends RuntimeException {
    public UsageLogNotFoundException(String message) {
        super(message);
    }
}
