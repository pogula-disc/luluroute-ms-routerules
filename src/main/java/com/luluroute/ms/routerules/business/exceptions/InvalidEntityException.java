package com.luluroute.ms.routerules.business.exceptions;

public class InvalidEntityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidEntityException() {
        super();
    }


    public InvalidEntityException(String message) {
        super(message);
    }

    public InvalidEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidEntityException(String message,
                                  Throwable cause,
                                  boolean enableSuppression,
                                  boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }
}
