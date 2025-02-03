package com.luluroute.ms.routerules.business.processors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.processors.helper.TForceECOMMRuleHelper;
/**
 * @author MANDALAKARTHIK1
 */
@Component
public class TForceECOMMRulesProcessor extends AbstractRulesProcessor {

	@Autowired
	private TForceECOMMRuleHelper rulesHelper;
	
    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile) {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);        
        rulesHelper.updatePlannedDeliveryDate(shipOptions,shipmentInfo,entityProfile);

    }
}
