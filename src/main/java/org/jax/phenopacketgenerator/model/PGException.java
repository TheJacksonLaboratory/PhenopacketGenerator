package org.jax.phenopacketgenerator.model;

public class PGException extends Exception {
    public PGException() {
        super();
    }

    public PGException(String message) {
        super(message);
    }

    public PGException(String message, Throwable cause) {
        super(message, cause);
    }

    public PGException(Throwable cause) {
        super(cause);
    }

    protected PGException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
