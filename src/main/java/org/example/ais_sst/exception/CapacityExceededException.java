package org.example.ais_sst.exception;

public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(String message) {
        super(message);
    }
}
