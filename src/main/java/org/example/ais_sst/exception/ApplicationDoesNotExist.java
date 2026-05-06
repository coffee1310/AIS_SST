package org.example.ais_sst.exception;

public class ApplicationDoesNotExist extends RuntimeException {
    public ApplicationDoesNotExist(String message) {
        super(message);
    }
}
