package com.luluroute.ms.routerules.business.exceptions;

import com.lululemon.wms.integration.common.exception.PermanentException;

public class MappingException extends PermanentException {
	private static final long serialVersionUID = 1L;

	public MappingException() {
		super();
	}

	public MappingException(String message) {
		super(message);
	}

	public MappingException(Throwable cause) {
		super(cause);
	}

	public MappingException(String message, Throwable cause) {
		super(message, cause);
	}

}
