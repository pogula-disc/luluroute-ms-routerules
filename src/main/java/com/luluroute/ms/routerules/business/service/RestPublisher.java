package com.luluroute.ms.routerules.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luluroute.ms.routerules.business.exceptions.ApplicationExceptions;
import com.luluroute.ms.routerules.business.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NoHttpResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.util.Arrays;
import java.util.Objects;

import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_ERROR;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_INFO;

import org.slf4j.MDC;
import java.util.*;

@Service
@Slf4j
public class RestPublisher<R,T> {

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    RestTemplate restTemplate;

    @Retryable(include = {HttpServerErrorException.class,
            UnknownHttpStatusCodeException.class,
            NoHttpResponseException.class},
            maxAttemptsExpression = "${restTemplate.maxAttempts:3}",
            recover = "handlePerformRestCallException")
    public R performRestAPICall(String url, String code, Class<R> responseType) {
        String msg = "RestPublisher.performRestCall()";

        log.info(String.format(Constants.STANDARD_INFO, msg, url, code));
        HttpHeaders headers = prepareHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<R> response =
                restTemplate.exchange(url + code, HttpMethod.GET, requestEntity, responseType);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error(String.format(Constants.STANDARD_INFO, msg, url, response));
        }

        return response.getBody();

    }

    @Retryable(include = {HttpServerErrorException.class,
            UnknownHttpStatusCodeException.class,
            NoHttpResponseException.class},
            maxAttemptsExpression = "${restTemplate.maxAttempts:3}",
            recover = "handlePerformRestCallException")
    public T performPostRestCall(String url, R data, ParameterizedTypeReference<T> responseType) {
        String msg = "RestPublisher.performRestCall()";
        try {
            log.info(String.format(STANDARD_INFO, msg, url + " - Request", mapper.writeValueAsString(data)));

            ResponseEntity<T> httpResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    prepareRequestData(data),
                    responseType);

            if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                log.error(String.format(STANDARD_INFO, msg, url, httpResponse));
            }

            T response =Objects.requireNonNull(httpResponse.getBody());

            log.info(String.format(STANDARD_INFO, msg, url + " - Response", mapper.writeValueAsString(response)));
            return response;
        } catch (JsonProcessingException jpe) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(jpe));
            throw new ApplicationExceptions("Json Processing Exception occurred: ", jpe);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Catch specific HTTP client and server errors and provide appropriate handling
            log.error(STANDARD_ERROR, msg, ex.getResponseBodyAsString());
            throw new ApplicationExceptions("HTTP Error occurred: " + ex.getMessage(), ex);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw new ApplicationExceptions("Unexpected error occurred: " + e.getMessage(), e);
        }
    }

    @Recover
    public Object handlePerformRestCallException(Exception exception, String url, R data){
        throw new ApplicationExceptions(exception.getMessage(), exception);
    }

    private HttpEntity<R> prepareRequestData(R data) {
        HttpHeaders headers = prepareHeaders();
        return new HttpEntity<>(data, headers);
    }

    public HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Map<String,String> contextMap = MDC.getCopyOfContextMap();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(Constants.X_CORRELATION_ID,contextMap.get(Constants.X_SHIPMENT_CORRELATION_ID));
        headers.set(Constants.IS_MOCK_ENABLED,contextMap.get(Constants.IS_MOCK_ENABLED));
        return headers;
    }
}
