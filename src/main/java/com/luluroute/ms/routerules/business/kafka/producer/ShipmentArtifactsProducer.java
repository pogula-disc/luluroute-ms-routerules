package com.luluroute.ms.routerules.business.kafka.producer;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static com.luluroute.ms.routerules.business.util.Constants.*;
import static com.luluroute.ms.routerules.business.util.ShipmentUtils.getStringValue;

@Slf4j
@Service
public class ShipmentArtifactsProducer {

    @Value("${config.producer.topic}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, ShipmentArtifact> kafkaTemplate;

    /**
     * Send Avro payload message to FactoryASN topic
     *
     * @param shipmentArtifact
     */
    public void sendPayload(ShipmentArtifact shipmentArtifact) {
        if(RequestContextHolder.getRequestAttributes() != null) {
            writeResponseToRestContext(shipmentArtifact);
        } else {
            writeResponseToKafka(shipmentArtifact);
        }
    }

    private void writeResponseToRestContext(ShipmentArtifact shipmentArtifact) {
        log.debug("Setting response for REST controller");
        HttpServletRequest httpServletRequest =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        httpServletRequest.setAttribute("shipmentArtifact", shipmentArtifact);
    }

    private void writeResponseToKafka(ShipmentArtifact shipmentArtifact) {
        String msg = "ShipmentArtifactProducer.sendPayload()";
        try {
            Headers headers = new RecordHeaders();
            headers.add(new RecordHeader(CARRIER_CODE,
                    toStringBytes(shipmentArtifact.getArtifactBody().getRouteRules().getRuleResult().getTargetCarrierCode())));

            kafkaTemplate.send(shipmentArtifactRecord(shipmentArtifact,
                    getStringValue(shipmentArtifact.getArtifactHeader().getShipmentCorrelationId()),
                    headers));
            log.info(MESSAGE_PUBLISHED,
                    shipmentArtifact.getArtifactHeader().getShipmentCorrelationId(),
                    shipmentArtifact, topicName);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private byte[] toStringBytes(Object object) {
        return String.valueOf(object).getBytes();
    }

    private <V> ProducerRecord<String, V> shipmentArtifactRecord(V value, String key, Headers headers) {
        return new ProducerRecord<>(topicName, null, key, value, headers);
    }

}
