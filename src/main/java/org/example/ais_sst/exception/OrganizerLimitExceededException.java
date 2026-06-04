package org.example.ais_sst.exception;

public class OrganizerLimitExceededException extends RuntimeException {
    public OrganizerLimitExceededException(String message) {
        super(message);
    }
}
