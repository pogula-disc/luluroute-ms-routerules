package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.luluroute.ms.routerules.business.util.Constants.TFORCE_ECOMM;

@Slf4j
@Component
public class TForceECOMMRulesProcessor extends AbstractRulesProcessor {

    private static final String CARRIER_CODE = TFORCE_ECOMM;
    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile) {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);
    }
}
