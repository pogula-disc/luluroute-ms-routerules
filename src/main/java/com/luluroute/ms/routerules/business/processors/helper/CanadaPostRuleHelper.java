package com.luluroute.ms.routerules.business.processors.helper;

import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.addShipOptionToErrors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.artifact.message.RulesInclude;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.AssignedTransitModes;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.rules.PlannedDeliveryDateRule;
import com.luluroute.ms.routerules.business.carrier.service.HolidayService;
import com.luluroute.ms.routerules.business.exceptions.DefaultTransitTimeFailureException;
import com.luluroute.ms.routerules.helper.TransitTimeHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CanadaPostRuleHelper {
	
	@Autowired
    private HolidayService holidayService;
	
	@Autowired
    private TransitTimeHelper transitTimeHelper;
	
	public void updatePlannedDeliveryDate(
            RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile){
        String msg = "updatePlannedDeliveryDate.updatePlannedDeliveryDate()";
        List<RulesInclude> validShipOptions = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            try {
                log.debug("Getting planned delivery date for Carrier Code: {} & Carrier Mode Code: {}",
                        shipOption.getTargetCarrierCode(), shipOption.getTargetCarrierModeCode());
                shipOption.setPlannedDeliveryDate(
                		getPlannedDeliveryDateForCanadaPost(shipOption, shipmentInfo,entityProfile));
                validShipOptions.add(shipOption);
            }catch (DefaultTransitTimeFailureException e) {
                addShipOptionToErrors(msg, shipOptions, shipOption, e);
            }
            catch (Exception e) {
                addShipOptionToErrors(msg, shipOptions, shipOption, e);
            }
        });
        shipOptions.setRulesInclude(validShipOptions);
    }
    
	public long getPlannedDeliveryDateForCanadaPost(
            RulesInclude shipOption, ShipmentInfo shipmentInfo,EntityPayload entityProfile ) throws DefaultTransitTimeFailureException{
		
		String orderType = shipmentInfo.getOrderDetails().getOrderType().toString();
		
		AssignedTransitModes assignedTransitModes = transitTimeHelper.getAssignedTransitModes(entityProfile, 
				shipOption.getTargetCarrierCode().toString(), shipOption.getTargetCarrierModeCode().toString(), orderType);
		int defaultTransitdays = assignedTransitModes.getDefaultTransitDays();
		List<String> holidays = holidayService.getHolidaySet(shipmentInfo, shipOption.getTargetCarrierCode().toString(),
				shipOption.getTargetCarrierModeCode().toString(), entityProfile.getTimezone());
		
		try {
			long dayMask = this.getDayCount(transitTimeHelper.isSaturdayDelivery(assignedTransitModes));
			return PlannedDeliveryDateRule.calculatePlannedDateOfDelivery(
					shipOption.getPlannedShipDate(),
					entityProfile.getTimezone(), "0", "0", dayMask, dayMask, dayMask,
					defaultTransitdays, defaultTransitdays,
					holidays);
		} catch (Exception e) {
			throw new DefaultTransitTimeFailureException(
					"Failed to calculate default Transit time .. Root cause " + ExceptionUtils.getStackTrace(e), e);
		}		
	}
	
	private long getDayCount(boolean isSaturdayDelivery) {
		return isSaturdayDelivery ? 126l : 62l;
	}
}
