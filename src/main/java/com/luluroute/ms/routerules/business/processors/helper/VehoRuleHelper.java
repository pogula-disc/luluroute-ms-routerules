package com.luluroute.ms.routerules.business.processors.helper;

import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.addShipOptionToErrors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.exceptions.DefaultTransitTimeFailureException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.service.RedisRehydrateService;
import com.luluroute.ms.routerules.helper.TransitTimeHelper;
import static com.luluroute.ms.routerules.business.util.Constants.ROUTE_RULES_ENTITY_NOT_FOUND;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_ERROR;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_SOURCE;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VehoRuleHelper {
	
	@Autowired
    private HolidayService holidayService;
	
	@Autowired
    private TransitTimeHelper transitTimeHelper;
	
	@Autowired
    private RedisRehydrateService redisRehyderateService;
	
	public void updatePlannedDeliveryDate(
            RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile,List<TransitTimeResponse> transitTimeResponses){
        String msg = "updatePlannedDeliveryDate.updatePlannedDeliveryDate()";
        List<RulesInclude> validShipOptions = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            try {
                log.debug("Getting planned delivery date for Carrier Code: {} & Carrier Mode Code: {}",
                        shipOption.getTargetCarrierCode(), shipOption.getTargetCarrierModeCode());
                shipOption.setPlannedDeliveryDate(
                		getPlannedDeliveryDateForVeho(shipOption, shipmentInfo,entityProfile,transitTimeResponses));
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
    
	public long getPlannedDeliveryDateForVeho(
            RulesInclude shipOption, ShipmentInfo shipmentInfo,EntityPayload entityProfile,List<TransitTimeResponse> transitTimeResponses ) throws DefaultTransitTimeFailureException{
		
		String orderType = shipmentInfo.getOrderDetails().getOrderType().toString();
		
		TransitTimeResponse transitTimeResponse = transitTimeResponses.stream()
                .filter(response -> isSameMode(response, shipOption)).findAny().orElse(null);
		int transitDays =0;
        if(transitTimeResponse != null && StringUtils.isEmpty(transitTimeResponse.getFailureMessage())) 
        	transitDays = transitTimeResponse.getTransitDays();
		
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
					transitDays, defaultTransitdays,
					holidays);
		} catch (Exception e) {
			throw new DefaultTransitTimeFailureException(
					"Failed to calculate default Transit time .. Root cause " + ExceptionUtils.getStackTrace(e), e);
		}		
	}
	
	private long getDayCount(boolean isSaturdayDelivery) {
		return isSaturdayDelivery ? 126l : 62l;
	}
	private static boolean isSameMode(
            TransitTimeResponse transitTimeResponse, RulesInclude shipOption) {
        return transitTimeResponse.getCarrierMode().equalsIgnoreCase(shipOption.getTargetCarrierModeCode().toString());
    }
	
	public EntityPayload loadEntityProfile(String entityCode) throws ShipmentMessageException, InterruptedException {
        String msg="VehoRuleHelper.loadEntityProfile()";
         try {
            EntityPayload entityProfile = redisRehyderateService.getEntityByCode(entityCode);
            log.debug(String.format(STANDARD_FIELD_INFO, "RouteRules - Entity Profile", entityProfile));

            if (ObjectUtils.isEmpty(entityProfile))
                throw new ShipmentMessageException(CODE_PROFILE, String.format(ROUTE_RULES_ENTITY_NOT_FOUND, entityCode), CODE_PROFILE_SOURCE);

            return entityProfile;
        } catch (Exception exp){
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(exp));
            throw new ShipmentMessageException(CODE_PROFILE, String.format(ROUTE_RULES_ENTITY_NOT_FOUND, entityCode), CODE_PROFILE_SOURCE);
        }
     }
}
