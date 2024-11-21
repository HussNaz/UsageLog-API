package com.example.UsageTrackAPI.exceptions;

public class LicenseValidationFailedException extends RuntimeException {
    public LicenseValidationFailedException(String message) {
        super(message);
    }
}
