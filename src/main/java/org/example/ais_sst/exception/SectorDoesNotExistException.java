package org.example.ais_sst.exception;

public class SectorDoesNotExistException extends RuntimeException {
    public SectorDoesNotExistException(String message) {
        super(message);
    }
}
