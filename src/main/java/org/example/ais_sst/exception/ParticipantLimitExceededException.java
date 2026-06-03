package org.example.ais_sst.exception;

public class ParticipantLimitExceededException extends RuntimeException {
    public ParticipantLimitExceededException(String message) {
        super(message);
    }
}
