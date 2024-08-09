package com.luluroute.ms.routerules.business.service;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.logistics.luluroute.avro.shipment.shared.ItemInfo;
import com.luluroute.ms.routerules.business.processors.BusinessRulesSelector;
import com.luluroute.ms.routerules.rule.ShipmentRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_UNKNOWN;
import static com.luluroute.ms.routerules.business.util.Constants.*;
import static com.luluroute.ms.routerules.business.util.ExceptionConstants.CODE_UNKNOWN_SOURCE;
import static com.luluroute.ms.routerules.business.util.ShipmentUtils.getStringValue;
import java.util.List;

@Service
@Slf4j
public class BusinessRouteRulesService {

    @Autowired
    private BusinessRuleArtifactService artifactService;
    @Autowired
    private BusinessRulesSelector businessRulesProcessor;

    private String messageCorrelationId = null;

    @Value("${mock.mockAttrKey:mock-api}")
    private String mockAttrKey;

    @Value("${mock.mockAttrValue:true}")
    private String mockAttrValue;

    @Async("CreateShipmentTaskExecutor")
    public void processRouteRulesRequest(ShipmentMessage message) {
        String msg = "BusinessRouteRulesService.processRouteRulesRequest()";
        try {
            ShipmentRule.validateShipmentMessage(message);
            messageCorrelationId = getStringValue(message.getMessageHeader().getMessageCorrelationId());
            MDC.put(X_MESSAGE_CORRELATION_ID, messageCorrelationId);
            MDC.put(IS_MOCK_ENABLED, String.valueOf(hasMockApiAttribute(message.getMessageHeader().getExtended())));
            MDC.put(X_PRIMARY_SOURCE_ENTITY, String.valueOf(
                    message.getMessageHeader().getMessageSources() != null &&
                            message.getMessageHeader().getMessageSources().size() > 0
                            ?
                    message.getMessageHeader().getMessageSources().get(0).getEntityCode()
                            :
                            EMPTY));

            message.getMessageBody().getShipments().forEach(shipmentInfo -> {
                try {
                    businessRulesProcessor.processRouteRules(shipmentInfo, messageCorrelationId);
                } catch (Exception e) {
                    log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
                    artifactService.buildAndSendErrorRouteRuleArtifact(shipmentInfo,
                            messageCorrelationId,
                            BUILD_ARTIFACT_ROUTE_RULE,
                            CODE_UNKNOWN,
                            ExceptionUtils.getStackTrace(e),
                            CODE_UNKNOWN_SOURCE);
                }
            });
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            artifactService.buildAndSendErrorRouteRuleArtifact(null,
                    messageCorrelationId,
                    BUILD_ARTIFACT_ROUTE_RULE,
                    CODE_UNKNOWN,
                    ExceptionUtils.getStackTrace(e),
                    CODE_UNKNOWN_SOURCE);
        }
    }

    public boolean hasMockApiAttribute(List<ItemInfo> itemInfoList) {
        boolean isMockEnabled = false;
        try {
            if (itemInfoList != null)
                isMockEnabled = itemInfoList.stream().anyMatch(
                        info -> mockAttrKey.equalsIgnoreCase(String.valueOf(info.getKey()))
                                && mockAttrValue.equalsIgnoreCase(String.valueOf(info.getValue())));
        } catch (Exception e) {
            log.error("Exception occurred in hasMockApiAttribute method {}", e.getMessage());
        }
        return isMockEnabled;
    }
}
