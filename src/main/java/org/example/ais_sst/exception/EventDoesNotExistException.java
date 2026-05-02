package org.example.ais_sst.exception;

public class EventDoesNotExistException extends RuntimeException {
    public EventDoesNotExistException(String message) {
        super(message);
    }
}
