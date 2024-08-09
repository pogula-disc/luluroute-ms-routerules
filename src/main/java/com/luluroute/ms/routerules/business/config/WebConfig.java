package com.luluroute.ms.routerules.business.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableRetry
public class WebConfig {

	@Value("${restTemplate.readTimeout}")
	private Integer readTimeout;

	@Value("${restTemplate.connectionTimeout}")
	private Integer connectionTimeout;

	@Bean
	public RestTemplate restTemplate() {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectionTimeout);
		requestFactory.setReadTimeout(readTimeout);
		RestTemplate template = new RestTemplate(requestFactory);
		MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
		jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		template.getMessageConverters().add(0, jsonHttpMessageConverter);

		return template;
	}

}