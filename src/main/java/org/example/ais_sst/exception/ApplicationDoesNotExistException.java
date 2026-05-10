package org.example.ais_sst.exception;

public class ApplicationDoesNotExistException extends RuntimeException {
    public ApplicationDoesNotExistException(String message) {
        super(message);
    }
}
