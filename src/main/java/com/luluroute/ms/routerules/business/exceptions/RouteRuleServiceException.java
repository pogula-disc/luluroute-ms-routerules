package com.luluroute.ms.routerules.business.exceptions;

public class RouteRuleServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RouteRuleServiceException() {
        super();
    }


    public RouteRuleServiceException(String message) {
        super(message);
    }

    public RouteRuleServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouteRuleServiceException(String message,
                                   Throwable cause,
                                   boolean enableSuppression,
                                   boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }
}