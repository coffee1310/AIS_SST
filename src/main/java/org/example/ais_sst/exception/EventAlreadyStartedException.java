package org.example.ais_sst.exception;

public class EventAlreadyStartedException extends RuntimeException {
    public EventAlreadyStartedException(String message) {
        super(message);
    }
}
