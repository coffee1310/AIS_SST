package org.example.ais_sst.exception;

public class EventNotActiveException extends RuntimeException {
    public EventNotActiveException(String message) {
        super(message);
    }
}
