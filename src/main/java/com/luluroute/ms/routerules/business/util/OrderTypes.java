package com.luluroute.ms.routerules.business.util;

import java.util.Arrays;
import java.util.List;

public enum OrderTypes {

	ALLOC, REPLEN, SPECIAL, TRUNK, ECOMM, STRAT, COMM, B2B;

	public static boolean isRetailOrder(String orderType) {
		List<String> retailOrderTypeList = Arrays.asList(ALLOC.name(), REPLEN.name(), SPECIAL.name(), TRUNK.name());
		return retailOrderTypeList.stream().anyMatch(orderType::equalsIgnoreCase);
	}

	public static boolean isEcommOrder(String orderType) {
		return ECOMM.name().equalsIgnoreCase(orderType);
	}

	public static boolean isStratOrder(String orderType) {
		List<String> stratOrderTypeList = Arrays.asList(STRAT.name(), COMM.name(), B2B.name());
		return stratOrderTypeList.stream().anyMatch(orderType::equalsIgnoreCase);
	}

}
