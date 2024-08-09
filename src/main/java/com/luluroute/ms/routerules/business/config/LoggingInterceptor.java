package com.luluroute.ms.routerules.business.config;

import com.luluroute.ms.routerules.business.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		String reqEntityCode = request.getHeader("X-RouteRules-Code");
		if (StringUtils.isNotBlank(reqEntityCode)) {
			MDC.put(Constants.CODE_PATH, reqEntityCode);
		} else {
			MDC.put(Constants.CODE_PATH, "user");
		}

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) {
		MDC.remove(Constants.CODE_PATH);
	}
}