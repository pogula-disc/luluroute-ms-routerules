package com.luluroute.ms.routerules;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.luluroute.ms.routerules, com.logistics.luluroute.carrier.fedex")
public class RouteRulesApplication {

	public static void main(String[] args) {
		SpringApplication.run(RouteRulesApplication.class, args);
	}

}
