package org.example.ais_sst.exception;

public class AccountCreatingRequestDoesNotExistException extends RuntimeException {
    public AccountCreatingRequestDoesNotExistException(String message) {
        super(message);
    }
}
