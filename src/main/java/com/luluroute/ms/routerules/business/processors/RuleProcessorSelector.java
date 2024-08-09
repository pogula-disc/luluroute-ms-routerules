package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_INFO;

@Slf4j
public class RuleProcessorSelector {

    private final Map<String, AbstractRulesProcessor> processors;

    public RuleProcessorSelector(Map<String, AbstractRulesProcessor> processors) {
        this.processors = processors;
    }

    /**
     * May update planned ship date and/or planned delivery date on each ship option (depending on the carrier).
     */
    public void updateShipOptionDates(
            Map<String, String> mdcContext, String carrierCode, RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile) {
        setMdcForThread(mdcContext);
        log.debug(String.format(STANDARD_INFO, "RuleProcessorSelector.updateShipOptionDates()",
                "Updating ship option dates for carrier #", carrierCode));
        processors.get(carrierCode).updateShipOptionDatesForCarrier(shipOptions, shipmentInfo, entityProfile);
    }

    public List<RateshopRatesDTO> updateShipOptionRates(
            Map<String, String> mdcContext, String carrierCode, RouteRules shipOptions, ShipmentInfo shipmentInfo)
            throws ShipmentMessageException {
        setMdcForThread(mdcContext);
        log.debug(String.format(STANDARD_INFO, "RuleProcessorSelector.loadRates()",
                "Updating ship option rates for carrier #", carrierCode));
        return processors.get(carrierCode).loadRates(shipOptions, shipmentInfo);
    }

    private static void setMdcForThread(Map<String, String> mdcContext) {
        if(mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
    }
}
