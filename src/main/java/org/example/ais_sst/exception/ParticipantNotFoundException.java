package org.example.ais_sst.exception;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(String message) {
        super(message);
    }
}
