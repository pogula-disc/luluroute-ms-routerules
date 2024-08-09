package com.luluroute.ms.routerules.business.exceptions;


    public class RedisKeyNotFoundException extends RuntimeException {

        
		private static final long serialVersionUID = 1L;

		public RedisKeyNotFoundException() {
            super();
        }


        public RedisKeyNotFoundException(String message) {
            super(message);
        }

        public RedisKeyNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public RedisKeyNotFoundException(String message,
                                         Throwable cause,
                                         boolean enableSuppression,
                                         boolean writableStackTrace) {

            super(message, cause, enableSuppression, writableStackTrace);
        }

    }

