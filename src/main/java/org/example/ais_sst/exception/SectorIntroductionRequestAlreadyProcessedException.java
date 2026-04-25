package org.example.ais_sst.exception;

public class SectorIntroductionRequestAlreadyProcessedException extends RuntimeException {
    public SectorIntroductionRequestAlreadyProcessedException(String message) {
        super(message);
    }
}
