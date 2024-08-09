package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.luluroute.ms.routerules.business.util.Constants.TFORCE_RETAIL;
import static com.luluroute.ms.routerules.business.util.Constants.UPS;

@Slf4j
@Component
public class TForceRetailRulesProcessor extends AbstractRulesProcessor {

    @Value("${config.serviceurl.tforce.retail.transit-time}")
    private String transitTimeUrl;
    private static final String CARRIER_CODE = TFORCE_RETAIL;

    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile)
            throws MappingFormatException, ShipmentMessageException {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);
        List<TransitTimeRequest> validTransitTimeRequests = super.getTransitTimeRequests(shipOptions, shipmentInfo,
                entityProfile);
        List<TransitTimeResponse> transitTimeResponses = super.callCarrierTransitTime(validTransitTimeRequests,
                transitTimeUrl);
        super.updatePlannedDeliveryDateForShipOptions(shipOptions, shipmentInfo, transitTimeResponses);
    }
}
