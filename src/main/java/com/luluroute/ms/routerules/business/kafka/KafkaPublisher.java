package com.luluroute.ms.routerules.business.kafka;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaPublisher {

    @Autowired
    private KafkaTemplate<String, ShipmentArtifact> kafkaTemplate;

    @Value("${config.producer.topic}")
    private String topicName;

    public void publishMessage(ShipmentArtifact artifactData) {
        log.debug("Trying to publish message to kafka topic {}", topicName);
        try {

        	kafkaTemplate.send(topicName, String.valueOf(artifactData.getArtifactHeader().getShipmentCorrelationId()),
                    artifactData);
        }
        
        catch (Exception e) {
        	
            log.error("Error publishing message to topic {}  ... failure message  {}", topicName,
                    ExceptionUtils.getStackTrace(e));
            throw e;
        }
       
        log.debug("Successfully published message.");
    }
}
