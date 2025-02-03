package com.luluroute.ms.routerules.business.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.artifact.message.RulesInclude;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.avro.shipment.shared.LocationItem;
import com.logistics.luluroute.redis.shipment.entity.AssignedTransitModes;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.rules.PlannedShipDateRule;
import com.luluroute.ms.routerules.business.carrier.service.HolidayService;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.mapper.ShipmentMessageMapper;
import com.luluroute.ms.routerules.business.service.RedisRehydrateService;
import com.luluroute.ms.routerules.business.service.RestPublisher;
import com.luluroute.ms.routerules.business.util.DateUtil;
import com.luluroute.ms.routerules.helper.TransitTimeHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.*;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.*;
import static com.luluroute.ms.routerules.business.util.Constants.*;

@Slf4j
@Component
public abstract class AbstractRulesProcessor {

    @Autowired
    private HolidayService holidayService;
    @Autowired
    private TransitTimeHelper transitTimeHelper;
    @Autowired
    RedisRehydrateService redisCacheLoader;
    @Autowired
    private RestPublisher<List<TransitTimeRequest>, List<TransitTimeResponse>> restPublisher;
    @Value("${config.instoreDate.applicable: true}")
    private boolean instoreDateApplicable;
    @Value("#{${config.instoreDate.orderTypes}}")
    private Map<String,String> instoreDateOrderTypes;
    @Value("${config.plannedDeliveryDate.override: true}")
    private boolean pddOverride;
    @Value("#{${config.plannedDeliveryDate.not-applicable-orderTypes}}")
    private  Map<String,String> pddNAOrderTypes;
    @Value("${config.feature.rateshop.zipcode-format.enabled}")
    protected boolean isZipCodeFormatEnabled;
    @Value("${config.feature.rateshop.zipcode-format.countries}")
    protected Set<String> zipCodeFormatCountries;
    @Value("${config.feature.error-override.date-validation.enabled}")
    protected boolean isErrorMessageOverrideEnabled;
    @Value("${config.feature.error-override.date-validation.lanes}")
    protected Set<String> errorMessageOverrideLanes;



    public List<RateshopRatesDTO> loadRates(RouteRules shipOptions, ShipmentInfo shipmentInfo)
            throws ShipmentMessageException {
        log.info(STANDARD_ERROR, "Rates", "Default rates");
        // Carrier Specific Rateshop need to override this method.
        //
        List<RateshopRatesDTO> rateshopRateDTOS = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            rateshopRateDTOS.add(RateshopRatesDTO.builder().baseRate(1.0)
                    .carrierCode(shipOption.getTargetCarrierCode().toString())
                    .modeCode(shipOption.getTargetCarrierModeCode().toString())
                    .build());
            shipOption.setBaseRate(1.0);
        });
        log.info("RateShopRatesDTOS in route rules: {}", rateshopRateDTOS);

        // For future carrier, if any common rate lookup
        return rateshopRateDTOS;

    }

    /**
     * May update planned ship date and/or planned delivery date on each ship option (depending on the carrier).
     *
     * @param shipOptions Should only be ship options that belong to the carrier
     */
    public void updateShipOptionDatesForCarrier(
            RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile) {
        try {
            updateShipOptionDates(shipOptions, shipmentInfo, entityProfile);
        } catch (ShipmentMessageException e) {
            eliminateAllShipOptionsForError(shipOptions, e);
        } catch (Exception e) {
            eliminateAllShipOptionsForError(shipOptions, e);
        }
    }

    /**
     * Each carrier's processor needs to extend this to set the planned ship date and/or planned delivery date for each
     * ship option if either date should deviate from what the user input in the shipment message.
     *
     * @throws MappingFormatException, JsonProcessingException, ParseException, ShipmentMessageException only when an
     * exception breaks all ship options for the carrier. Otherwise, exceptions are handled per ship option.
     */
    public abstract void updateShipOptionDates(
            RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile)
            throws MappingFormatException, JsonProcessingException, ParseException, ShipmentMessageException;

    public void updateNoDatesForShipOptions(RouteRules shipOptions) {
        String msg = "AbstractRulesProcessor.updateNoDatesForShipOptions()";
        List<RulesInclude> validShipOptions = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            try {
                log.debug("Does not update dates - Carrier Code {} & Carrier Mode Code: {}",
                        shipOption.getTargetCarrierCode(), shipOption.getTargetCarrierModeCode());
                validShipOptions.add(shipOption);
            } catch (Exception e) {
                addShipOptionToErrors(msg, shipOptions, shipOption, e);
            }
        });

        shipOptions.setRulesInclude(validShipOptions);
    }

    /**
     * Adjusts planned ship dates for holidays, etc. on all ship options
     */
    public void updatePlannedShipDateForShipOptions(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile) {
        String msg = "AbstractRulesProcessor.updatePlannedShipDateForShipOptions()";
        List<RulesInclude> validShipOptions = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            try {
                log.debug("Updating ship option dates for Carrier Code: {} & Carrier Mode Code: {}",
                        shipOption.getTargetCarrierCode(), shipOption.getTargetCarrierModeCode());
                long pickUpDate = getPlannedShipDateForShipOption(shipmentInfo, entityProfile,
                        shipOption.getTargetCarrierCode().toString(), shipOption.getTargetCarrierModeCode().toString());
                shipOption.setPlannedShipDate(pickUpDate);
                log.debug("{} - Calculated Planned Ship Date: {}", msg, pickUpDate);
                validShipOptions.add(shipOption);
            } catch (ShipmentMessageException exp) {
                addShipOptionToErrors(msg, shipOptions, shipOption, exp);
            } catch (Exception e) {
                addShipOptionToErrors(msg, shipOptions, shipOption, e);
            }
        });
        shipOptions.setRulesInclude(validShipOptions);
    }

    /**
     * @return Builds transit time API requests for all ship options
     */
    public List<TransitTimeRequest> getTransitTimeRequests(
            RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile) throws MappingFormatException {
        String msg = "AbstractRulesProcessor.getTransitTimeRequests()";
        com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo shipmentInfoAPI =
                ShipmentMessageMapper.INSTANCE.mapShipmentMessage(shipmentInfo);
        List<TransitTimeRequest> transitTimeRequests = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            try {
                String modeCode = shipOption.getTargetCarrierModeCode().toString();
                String carrierCode = shipOption.getTargetCarrierCode().toString();
                log.debug("Building transit time request for Carrier Code: {} & Carrier Mode Code: {}", modeCode, carrierCode);
                String orderType = shipmentInfo.getOrderDetails().getOrderType().toString();
                AssignedTransitModes assignedTransitModes = transitTimeHelper.getAssignedTransitModes(entityProfile, carrierCode, modeCode, orderType);
                TransitTimeRequest requests = transitTimeHelper.prepareTransitTimeRequestData(
                        shipOption.getPlannedShipDate(), shipmentInfoAPI, entityProfile, assignedTransitModes, modeCode);
                transitTimeRequests.add(requests);
            } catch (Exception e) {
                log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
                // ship option gets moved to errors while matching transit time to ship option
            }
        });
        return transitTimeRequests;
    }

    /**
     * Adjusts planned delivery dates on all ship options based on carrier ms's response
     */
    public void updatePlannedDeliveryDateForShipOptions(
            RouteRules shipOptions, ShipmentInfo shipmentInfo, List<TransitTimeResponse> transitTimeResponses) {
        String msg = "AbstractRulesProcessor.updatePlannedDeliveryDateForShipOptions()";
        List<RulesInclude> validShipOptions = new ArrayList<>();
        long plannedDeliveryDate = getPlannedDeliveryDate(shipmentInfo);
        log.info(String.format(STANDARD_FIELD_INFO, "Updated PDD", plannedDeliveryDate));
        shipOptions.getRulesInclude().forEach(shipOption -> {
            try {
                log.debug("Getting planned delivery date for Carrier Code: {} & Carrier Mode Code: {}",
                        shipOption.getTargetCarrierCode(), shipOption.getTargetCarrierModeCode());
                shipOption.setPlannedDeliveryDate(
                        getPlannedDeliveryDateForShipOption(transitTimeResponses, plannedDeliveryDate, shipOption, shipmentInfo));
                validShipOptions.add(shipOption);
            } catch (ShipmentMessageException exp) {
                addShipOptionToErrors(msg, shipOptions, shipOption, exp);
            } catch (Exception e) {
                addShipOptionToErrors(msg, shipOptions, shipOption, e);
            }
        });
        shipOptions.setRulesInclude(validShipOptions);
    }

    /**
     * Adjust PSD based on mask and holidays
     */
    private long getPlannedShipDateForShipOption(
            ShipmentInfo shipmentInfo, EntityPayload entityProfile, String carrierCode, String carrierMode) throws ShipmentMessageException {
        String msg = "AbstractRulesProcessor.getPlannedShipDateForShipOption()";

        try {
            log.debug("Getting ship date for Carrier Code: {} & Carrier Mode Code: {}", carrierCode, carrierMode);
            long plannedShipDate = checkAndUpdatePlannedShippedDate(shipmentInfo.getTransitDetails().getDateDetails().getPlannedShipDate() , entityProfile.getTimezone());

            List<String> holidays = holidayService.getHolidaySet(shipmentInfo, carrierCode, carrierMode, entityProfile.getTimezone());
            String orderType = shipmentInfo.getOrderDetails().getOrderType().toString();
            AssignedTransitModes assignedTransitModes = transitTimeHelper.getAssignedTransitModes(entityProfile, carrierCode, carrierMode, orderType);

            log.debug("getTimezone# {} getCutOffHH # {} getCutOffMM # {} getPickupDaysMask # {}", entityProfile.getTimezone(),
                    assignedTransitModes.getCutOffHH(),
                    assignedTransitModes.getCutOffMM(),
                    assignedTransitModes.getPickupDaysMask());

            long calculatedPlannedShipDate = PlannedShipDateRule.calculatePlannedShipDate(plannedShipDate,
                    entityProfile.getTimezone(),
                    assignedTransitModes.getCutOffHH(),
                    assignedTransitModes.getCutOffMM(),
                    assignedTransitModes.getPickupDaysMask(),
                    holidays);

            log.debug(String.format(STANDARD_FIELD_INFO, "calculatedPlannedShipDate", calculatedPlannedShipDate));

            return calculatedPlannedShipDate;

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw new ShipmentMessageException(CODE_PSD_ERROR, CODE_CODE_PSD_CALCULATIONS, CODE_CARRIER_SELECTION_SOURCE);
        }
    }

    private static long checkAndUpdatePlannedShippedDate(long plannedShipDate,  String entityTimeZone) {
        log.warn(String.format(STANDARD_FIELD_INFO, "PlannedShipDate # ", plannedShipDate));
        // Handle NULL/Empty/0 Planned Shipped Date
        if (plannedShipDate <= 0) {
            plannedShipDate = DateUtil.currentDateTimeInLong(entityTimeZone);
            log.warn(String.format(STANDARD_FIELD_INFO, "PlannedShipDate Not Available.Updated PlannedShipDate #", plannedShipDate));
        }
        return plannedShipDate;
    }

    public boolean updateRateForShipOption(String carrierCode, RateshopRatesDTO rateshopRatesMode, RouteRules shipOptions) {
        for (RulesInclude shipOption : shipOptions.getRulesInclude()) {
            if (carrierCode.equalsIgnoreCase(shipOption.getTargetCarrierCode().toString())
                    && rateshopRatesMode.getModeCode().equalsIgnoreCase(shipOption.getTargetCarrierModeCode().toString())) {
                shipOption.setBaseRate(rateshopRatesMode.getBaseRate());
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    public String formatToFiveDigitZipCode(LocationItem address, boolean formatEnabled, Set<String> formatCountries) {
        String countryCode = String.valueOf(address.getCountry());
        String zipCodeStr = String.valueOf(address.getZipCode());
        if (formatEnabled && formatCountries.contains(countryCode)) {
            log.debug("Format ZipCode Input: {}", zipCodeStr);
            String[] zipCodeInfo = zipCodeStr.split("-");
            if (zipCodeInfo[0].length() == 5) {
                log.debug("Formatted to Five Digit ZipCode: {}", zipCodeInfo[0]);
                return zipCodeInfo[0];
            }
        }
        return zipCodeStr;
    }


    public void eliminateShipOptionsWithoutRates(RouteRules shipOptions) {
        String msg = "AbstractRulesProcessor.eliminateShipOptionsWithoutRates()";
        List<RulesInclude> validShipOptions = new ArrayList<>();
        shipOptions.getRulesInclude().forEach(shipOption -> {
            if(shipOption.getBaseRate() == 0) {
                addShipOptionToErrors(msg, shipOptions, shipOption, getExceptionForNoRates(
                        shipOption.getTargetCarrierCode().toString(), shipOption.getTargetCarrierModeCode().toString()));
            } else {
                validShipOptions.add(shipOption);
            }
        });
        shipOptions.setRulesInclude(validShipOptions);
    }


    public List<TransitTimeResponse> callCarrierTransitTime(List<TransitTimeRequest> transitTimeRequests, String transitTimeUrl)
            throws ShipmentMessageException {
        String methodName = "AbstractRulesProcessor.callCarrierTransitTime()";
        List<TransitTimeResponse> transitTimeResponses = new ArrayList<>();
        try {
            log.debug(methodName);
            if (!CollectionUtils.isEmpty(transitTimeRequests)) {
                transitTimeResponses = restPublisher.performPostRestCall(transitTimeUrl, transitTimeRequests, new ParameterizedTypeReference<>() {
                });
                log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime-transitTimeResponses", transitTimeResponses));
            }
        } catch (Exception e) {
            log.error(STANDARD_ERROR, methodName, ExceptionUtils.getStackTrace(e));
            throw new ShipmentMessageException(
                    CODE_TRANSIT_TIME,
                    ROUTE_RULES_TRANSIT_TIME_ERROR,
                    CODE_TRANSIT_TIME_SOURCE);
        }
        return transitTimeResponses;
    }

    /**
     * Adjust PDD based on carrier ms's response
     */
    protected long getPlannedDeliveryDateForShipOption(
            List<TransitTimeResponse> transitTimeResponses, long plannedDeliveryDate, RulesInclude shipOption, ShipmentInfo shipmentInfo)
            throws ShipmentMessageException {
        String carrierCode = shipOption.getTargetCarrierCode().toString();
        String modeCode = shipOption.getTargetCarrierModeCode().toString();
        TransitTimeResponse transitTimeResponse = transitTimeResponses.stream()
                .filter(response -> isSameMode(response, shipOption)).findAny().orElse(null);
        String responseFailureMessage = null;
        long carrierDeliveryDate = -1;
        if(transitTimeResponse != null && StringUtils.isEmpty(transitTimeResponse.getFailureMessage())) {
            carrierDeliveryDate = transitTimeResponse.getResponseDeliveryDate();
            log.debug("Selected Carrier# {} & Service Mode# {}", carrierCode, modeCode);
            log.debug(" Input PlannedDeliveryDate {} , Calculated PlannedDeliveryDate: {}", plannedDeliveryDate, carrierDeliveryDate);

            if (checkRuleMatchPriorityDate(carrierDeliveryDate, plannedDeliveryDate)) {
                // PDD override with PDD calculated with carrier transit
                // Except ALLOC/SPECIAL order by as per configured DC
                if (pddOverride) {
                    String DCs =  pddNAOrderTypes.get(String.valueOf(shipmentInfo.getOrderDetails().getOrderType()));
                    if (null != DCs && DCs.contains(String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode())))
                        return plannedDeliveryDate;
                }

                log.debug(String.format(STANDARD_FIELD_INFO, "Overriding PlannedDeliveryDate", transitTimeResponse.getResponseDeliveryDate()));
                return transitTimeResponse.getResponseDeliveryDate();
            }
        }

        if (transitTimeResponse != null) {
            responseFailureMessage = transitTimeResponse.getFailureMessage() != null ?
                    transitTimeResponse.getFailureMessage() :
                    String.format(CARRIER_MODE,
                            shipOption.getTargetCarrierCode(),
                            shipOption.getTargetCarrierModeCode());

        }

        log.warn(String.format(STANDARD_ERROR,"responseFailureMessage",responseFailureMessage));

        String error = String.format(ROUTE_RULES_NOT_FOUND_CALC, appendToErrorMessage(shipmentInfo), plannedDeliveryDate, carrierDeliveryDate, responseFailureMessage);
        throw new ShipmentMessageException(CODE_PROFILE_DATE_CALC, error, CODE_NO_DATA_DATE_CALC_SOURCE);
    }

    private String appendToErrorMessage(ShipmentInfo  shipmentInfo){
        log.info(String.format(STANDARD_FIELD_INFO,"errorMessageOverrideLanes",errorMessageOverrideLanes));
        if(isErrorMessageOverrideEnabled && errorMessageOverrideLanes.contains(String.valueOf(shipmentInfo.getOrderDetails().getLaneName())))
            return ROUTE_RULES_NOT_RECOMMENDED_SERVICE_FOUND;
        return STRING_EMPTY;
    }

    private long getPlannedDeliveryDate(ShipmentInfo shipmentInfo) {
        if (instoreDateApplicable && instoreDateOrderTypes.containsKey(String.valueOf(shipmentInfo.getOrderDetails().getOrderType()))){

            String applicableDCs =   instoreDateOrderTypes.get(String.valueOf(shipmentInfo.getOrderDetails().getOrderType()));
            String originDC =  String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode());
            log.info("PDD override - config DCs # {} originDC # {} ", applicableDCs , originDC );
            if (StringUtils.isNotEmpty(originDC) && applicableDCs.contains(originDC) &&
                        shipmentInfo.getTransitDetails().getDateDetails().getInStoreDate() > 0)
                                return shipmentInfo.getTransitDetails().getDateDetails().getInStoreDate();
        }
        //  as-is PlannedDeliveryDate
        return shipmentInfo.getTransitDetails().getDateDetails().getPlannedDeliveryDate();
    }

    private static boolean isSameMode(
            TransitTimeResponse transitTimeResponse, RulesInclude shipOption) {
        return transitTimeResponse.getCarrierMode().equalsIgnoreCase(shipOption.getTargetCarrierModeCode().toString());
    }

    /**
     * compare dates
     */
	private static boolean checkRuleMatchPriorityDate(long responseDeliveryDate, long plannedDeliveryDate) {
		// Adding condition plannedDeliveryDate == 0 to handle situation where SFS
		// orders don't send PDD and we need to get date from carrier
		return (plannedDeliveryDate == 0)
				|| (responseDeliveryDate > 0 && (responseDeliveryDate <= plannedDeliveryDate));
	}
}