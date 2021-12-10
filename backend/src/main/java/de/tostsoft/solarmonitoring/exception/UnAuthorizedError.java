package de.tostsoft.solarmonitoring.exception;

public class UnAuthorizedError extends RuntimeException {
    public UnAuthorizedError(String message) {
        super(message);
    }
}
