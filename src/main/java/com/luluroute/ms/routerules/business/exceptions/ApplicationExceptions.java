package com.luluroute.ms.routerules.business.exceptions;

public class ApplicationExceptions extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ApplicationExceptions() {
		super();
	}

	public ApplicationExceptions(String message, Throwable cause) {
		super(message, cause);
	}

	public ApplicationExceptions(String message) {
		super(message);
	}

	public ApplicationExceptions(Throwable cause) {
		super(cause);
	}

}