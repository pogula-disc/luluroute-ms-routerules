package com.luluroute.ms.routerules.business.exceptions;

public class ConfigurationMissingException extends  RuntimeException {

    
	private static final long serialVersionUID = 1L;

	public ConfigurationMissingException(String message) {
        super(message);
    }

    public ConfigurationMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationMissingException(String message,
                                         Throwable cause,
                                         boolean enableSuppression,
                                         boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }

}
