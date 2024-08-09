package com.luluroute.ms.routerules.business.kafka.consumer;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.lululemon.wms.integration.common.exception.PermanentException;
import com.lululemon.wms.integration.common.exception.TransientException;
import com.lululemon.wms.integration.common.kafka.AbstractTopicConsumer;
import com.lululemon.wms.integration.common.kafka.EventHeaders;
import com.luluroute.ms.routerules.business.service.BusinessRouteRulesService;
import com.luluroute.ms.routerules.business.util.Constants;
import com.luluroute.ms.routerules.business.util.ShipmentUtils;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.luluroute.ms.routerules.business.util.Constants.*;
import static com.luluroute.ms.routerules.business.util.ShipmentUtils.getStringValue;

@Slf4j
@Component
public class ShipmentMessageConsumer extends AbstractTopicConsumer<ShipmentMessage> {

	@Autowired
	private BusinessRouteRulesService service;
	@Autowired
	private Counter successCounter;

		public ShipmentMessageConsumer(@Value("${config.shipment.topic}") String mainTopicName,
							   @Value("${config.shipment.topic}") String retryTopicName,
							   @Value("${config.shipment.topic}") String dlqTopicName) {
		super(mainTopicName, null, null);
	}

	@Override
	@KafkaListener(id = "${config.shipment.consumerGroup}", topics = "${config.shipment.topic}", groupId = "${config.shipment.consumerGroup}")
	public void consumeMessage(@Payload ShipmentMessage message, @Headers MessageHeaders headers, Acknowledgment ack) {
		super.consumeMessage(message, headers, ack);
	}

	@Override
	@Scheduled(fixedDelayString = "${config.error.processing.scheduler.rate}")
	public void checkForResume() {
		super.checkForResume();
	}

	@Override
	public void processMessageImpl(ShipmentMessage message, EventHeaders headers) throws PermanentException, TransientException {
		String msg = "ShipmentMessageConsumer.processMessageImpl()";
		if(message != null && message.getRequestHeader() != null && message.getMessageBody() != null) {
			log.info(String.format(Constants.MESSAGE, Constants.ROUTE_RULES_CONSUMER, message));
			try{
				switch (getStringValue(message.getRequestHeader().getRequestType())) {
					case SHIPMENT_REQUEST_RELEASE:
						service.processRouteRulesRequest(message);
						successCounter.increment();
						break;
					default:
						log.warn(STANDARD_WARN, msg,
								String.format("RouteRules - NOT A Shipment Request-Release(2000 # {%s}", message));
				}
			}catch (Exception e) {
				log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
			}
		} else {
			log.error(STANDARD_ERROR, msg, String.format("RouteRules - INVALID NULL Message {%s}", message));
		}
		MDC.clear();
	}

	@Override
	public void addLoggingContext(ShipmentMessage message, MessageHeaders headers) {
		log.info("APP_MESSAGE=\"Received AVRO message\" | message=\"{}\"", message);
		MDC.put(Constants.X_CORRELATION_ID, ShipmentUtils.getCorrelationId());
	}

	@Override
	public void removeLoggingContext() {
		MDC.remove(Constants.X_CORRELATION_ID);
		MDC.remove(Constants.X_TRANSACTION_REFERENCE);

	}
}




