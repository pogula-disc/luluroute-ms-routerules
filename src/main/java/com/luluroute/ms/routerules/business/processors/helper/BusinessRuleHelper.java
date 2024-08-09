package com.luluroute.ms.routerules.business.processors.helper;

import com.logistics.luluroute.avro.artifact.message.*;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.mapper.RulesMapper;
import com.luluroute.ms.routerules.business.service.BusinessRuleArtifactService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.*;
import static com.luluroute.ms.routerules.business.service.BusinessRuleArtifactService.buildEmptyRouteRules;
import static com.luluroute.ms.routerules.business.util.Constants.*;

@Slf4j
@Component
public class BusinessRuleHelper {

    @Autowired
    private BusinessRuleArtifactService artifactService;

    public void sendErrorArtifact(ShipmentInfo shipmentInfo, String messageCorrelationId) {
        artifactService.buildAndSendErrorRouteRuleArtifact(shipmentInfo,
                messageCorrelationId,
                BUILD_ARTIFACT_ROUTE_RULE,
                CODE_CARRIER_SELECTION,
                ROUTE_RULES_NOT_PROCESSES,
                CODE_CARRIER_SELECTION_SOURCE);
    }

    public void sendErrorArtifact(ShipmentInfo shipmentInfo, String messageCorrelationId, RouteRules routeRules) {
        artifactService.buildAndSendErrorRouteRuleArtifact(shipmentInfo,
                messageCorrelationId,
                BUILD_ARTIFACT_ROUTE_RULE,
                routeRules);
    }

    public void sendErrorArtifact(ShipmentInfo shipmentInfo, String messageCorrelationId, ShipmentMessageException e) {
        artifactService.buildAndSendErrorRouteRuleArtifact(shipmentInfo,
                messageCorrelationId,
                BUILD_ARTIFACT_ROUTE_RULE,
                e.getCode(),
                e.getDescription(),
                e.getSource());
    }

    public static void addShipOptionToErrors(String msg, RouteRules shipOptions, RulesInclude brokenShipOption, ShipmentMessageException e) {
        addShipOptionToErrors(msg, shipOptions, brokenShipOption, e, e.getCode(), e.getDescription(), e.getSource());
    }

    public static void addShipOptionToErrors(String msg, RouteRules shipOptions, RulesInclude brokenShipOption, Exception e) {
        addShipOptionToErrors(msg, shipOptions, brokenShipOption, e, CODE_UNKNOWN, e.getMessage(), CODE_UNKNOWN_SOURCE);
    }

    public static void addShipOptionToErrors(
            String msg, RouteRules shipOptions, RulesInclude brokenShipOption, Exception e, String errorCode, String errorMessage, String errorSource) {
        log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
        RulesError rulesError = RulesMapper.INSTANCE.mapShipOptionToError(brokenShipOption, errorCode, errorMessage, errorSource);
        if(ObjectUtils.isEmpty(rulesError.getResponseNotes())) {
            rulesError.setResponseNotes("");
        }
        shipOptions.getRulesError().add(rulesError);
    }

    /**
     * Something went wrong enough that we're eliminating all routes on this carrier
     */
    public static void eliminateAllShipOptionsForError(RouteRules shipOptions, Exception e) {
        String methodName = "RuleProcessorSelector.eliminateAllShipOptionsForError()";

        shipOptions.getRulesInclude().forEach(shipOption ->
                addShipOptionToErrors(methodName, shipOptions, shipOption, e));
        shipOptions.setRulesInclude(List.of());
    }

    public static void eliminateAllShipOptionsForError(RouteRules shipOptions, ShipmentMessageException e) {
        String methodName = "RuleProcessorSelector.eliminateAllShipOptionsForError()";

        shipOptions.getRulesInclude().forEach(shipOption ->
                addShipOptionToErrors(methodName, shipOptions, shipOption, e));
        shipOptions.setRulesInclude(List.of());
    }

    public static ShipmentMessageException getExceptionForNoRates(String carrierCode, String modeCode) {
        return new ShipmentMessageException(CODE_PROFILE_RATE,
                String.format(RATE_NOT_FOUND, carrierCode, modeCode, null),
                CODE_NO_DATA_RATE_SOURCE);
    }

    public static RouteRules condenseRulesForAllCarriers(Map<String, RouteRules> routeRulesByCarrier) {
        RouteRules condensedRouteRules = buildEmptyRouteRules();
        routeRulesByCarrier.keySet().forEach(carrierCode -> {
            RouteRules carrierRule = routeRulesByCarrier.get(carrierCode);
            condensedRouteRules.getRulesInclude().addAll(carrierRule.getRulesInclude());
            condensedRouteRules.getRulesError().addAll(carrierRule.getRulesError());
        });
        return condensedRouteRules;
    }

    public static RouteRules buildRouteRulesWithError(String errorCode, String errorMessage, String errorSource) {
        RulesError rulesError = RulesError.newBuilder()
                .setRouteRuleId(STRING_EMPTY)
                .setRuleCode(STRING_EMPTY)
                .setRuleType(STRING_EMPTY)
                .setApplyType(STRING_EMPTY)
                .setApplyType(STRING_EMPTY)
                .setDescription(STRING_EMPTY)
                .setPriority(0)
                .setTargetCarrierCode(STRING_EMPTY)
                .setTargetCarrierModeCode(STRING_EMPTY)
                .setName(STRING_EMPTY)
                .setResponseNotes(STRING_EMPTY)
                .setApplicables(new ArrayList<>())
                .setProcessException(ProcessException.newBuilder()
                        .setCode(errorCode)
                        .setDescription(errorMessage)
                        .setSource(errorSource)
                        .build())
                .build();

        List<RulesError> errorList = new ArrayList<>();
        errorList.add(rulesError);

        return RouteRules.newBuilder()
                .setRulesOveride(new ArrayList<>())
                .setRulesInclude(new ArrayList<>())
                .setRulesExclude(new ArrayList<>())
                .setRulesError(errorList)
                .setRuleResult(RuleResult.newBuilder()
                        .setId(STRING_EMPTY)
                        .setType(STRING_EMPTY)
                        .setCode(STRING_EMPTY)
                        .setTargetCarrierCode(STRING_EMPTY)
                        .setTargetCarrierModeCode(STRING_EMPTY)
                        .setResponseNotes(STRING_EMPTY)
                        .setApplicables(new ArrayList<>()).build())
                .build();
    }
}
