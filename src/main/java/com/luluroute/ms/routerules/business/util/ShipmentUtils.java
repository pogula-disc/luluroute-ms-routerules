package com.luluroute.ms.routerules.business.util;

import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Supplier;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_SOURCE;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.util.ExceptionConstants.CODE_PROFILE;

@Slf4j
public class ShipmentUtils {
	
	public static String getCorrelationId() {
		return UUID.randomUUID().toString();
	}
	
	public static void clearMDC() {
		MDC.remove(Constants.X_CORRELATION_ID);
		MDC.remove(Constants.X_TRANSACTION_REFERENCE);
	}

	public static <T> String getStringValue(T input) {
		return null != input ? String.valueOf(input) : null;
	}

	public static <T> T retry(Supplier<T> supplier, int maxRetries, long delayMillis) throws ShipmentMessageException, InterruptedException {
		int retryCount = 0;
		T value;

		while (retryCount < maxRetries) {
			try {
				value = supplier.get();
				log.debug(String.format(STANDARD_FIELD_INFO, "EntityProfile - Redis Cache Value", value));
				if(value == null) {
					throw new ShipmentMessageException(CODE_PROFILE, "RouteRules - No Entity Profile from Redis Cache", CODE_PROFILE_SOURCE);
				}
				return value;
			} catch (ShipmentMessageException e) {
				retryCount++;
				log.debug(String.format(STANDARD_FIELD_INFO, "Retry Count", retryCount));
				if (retryCount == maxRetries) {
					throw e;
				}
				Thread.sleep(delayMillis);
			}
		}
		return null;
	}
}
