package com.luluroute.ms.routerules.business.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	public static final String ROUTERULES_SVC_TAG = "RouteRules Service";

	@Value("${lulu-route.routerules-svc.swagger.host}")
	private String host;

	@Bean
	public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
		return new BeanPostProcessor() {

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
					try {
						customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
					} catch (Exception e) {
						log.error("Exception occured from postProcessAfterInitialization {}  ", "Exception",
			                    ExceptionUtils.getStackTrace(e));
		
					}
				}
				return bean;
			}

			private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
				List<T> copy = mappings.stream()

						.filter(mapping -> mapping.getPatternParser() == null).collect(Collectors.toList());
				mappings.clear();
				mappings.addAll(copy);
			}

			@SuppressWarnings("unchecked")
			private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) throws Exception {
				try {
					Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
					assert field != null;
					field.setAccessible(true);
					return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					
					log.error("Exception occured from getHandlerMappings {}  ... failure message  {}", "IllegalStateException",
		                    ExceptionUtils.getStackTrace(e));
		           throw e;
				}
			}
		};
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).host(host).select()
				.apis(RequestHandlerSelectors.basePackage("com.luluroute.ms.routerules")).paths(PathSelectors.any())
				.build().apiInfo(apiInfo()).pathMapping("/").enable(true)
				.tags(new Tag(ROUTERULES_SVC_TAG, "This service used to routerules"));
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("RouteRules Service API")
				.description("This is REST API containing all the operations related to RouteRules")
				.contact(new Contact("Luluroute 2.0", "", "support@lululemon.com")).version("1.0.0").build();
	}
}