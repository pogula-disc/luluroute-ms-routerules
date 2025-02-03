package com.luluroute.ms.routerules.business.service;

import com.google.gson.Gson;
import com.logistics.luluroute.avro.artifact.message.*;
import com.logistics.luluroute.avro.shipment.service.ShipmentDatesInfo;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;
import com.luluroute.ms.routerules.business.kafka.producer.ShipmentArtifactsProducer;
import com.luluroute.ms.routerules.business.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_ARTIFACTS;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_ARTIFACTS_SOURCE;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.buildRouteRulesWithError;
import static com.luluroute.ms.routerules.business.util.Constants.*;
import static com.luluroute.ms.routerules.business.util.ShipmentUtils.getCorrelationId;
import static java.lang.Boolean.FALSE;

@Slf4j
@Service
public class BusinessRuleArtifactService {

    @Autowired
    private ShipmentArtifactsProducer artifactProducer;

    public void buildAndSendRouteRuleArtifact(ShipmentInfo shipmentInfo,
                                              RouteRules shipOptions,
                                              List<RateshopRatesDTO> rateShopRates, String messageCorrelationId) {
        String msg = "BusinessRuleArtifactService.buildAndSendRouteRuleArtifact()";
        try {
            ArtifactHeader artifactHeader = ArtifactHeader.newBuilder()
                    .setArtifactCreationDay(DateUtil.currentDateTimeInLong())
                    .setArtifactType(BUILD_ARTIFACT_ROUTE_RULE)
                    .setArtifactStatus(BUILD_ARTIFACT_STATUS_COMPLETE)
                    .setShipmentArtifactId(getCorrelationId())
                    .setMessageCorrelationId(messageCorrelationId)
                    .setShipmentCorrelationId(shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                    .setProcesses(buildProcessDetails(false, null))
                    .build();

            RulesInclude selectedShipOption = shipOptions.getRulesInclude().get(0);
            shipOptions.setRuleResult(buildRuleResult(selectedShipOption));

            // Need to update based on Avro schema
            TransitTimes transitTimes = TransitTimes.newBuilder()
                    .setTransitTimeId(String.valueOf(UUID.randomUUID()))
                    .setTransitTimeHashKey(STRING_EMPTY)
                    .setCarrierType(STRING_EMPTY)
                    .setCarrierCode(selectedShipOption.getTargetCarrierCode())
                    .setMode(selectedShipOption.getTargetCarrierModeCode())
                    .setTransitDays(0)
                    .setCutOffTimeHH(STRING_EMPTY)
                    .setCutOffTimeMM(STRING_EMPTY)
                    .setCutOffTimeApplied(FALSE)
                    .setResponseType(STRING_EMPTY)
                    .setRequestShipDate(0)
                    .setPickUpDate(selectedShipOption.getPlannedShipDate())
                    .setPlannedDeliveryDate(selectedShipOption.getPlannedDeliveryDate())
                    .setDeliveryDate(0)
                    .setNotes(STRING_EMPTY)
                    .setTransitTimeExtended(new ArrayList<>())
                    .build();

            List<TransitTimes> transitTimesList = new ArrayList<>();
            transitTimesList.add(transitTimes);

            log.info("{}", String.format(STANDARD_FIELD_INFO, "RateShopRates", rateShopRates));
            List<Rates> rates = rateShopRates.stream()
                    .map(rate -> Rates.newBuilder()
                            .setCarrierCode(rate.getCarrierCode())
                            .setMode(rate.getModeCode())
                            .setBaseCost(rate.getBaseRate())
                            .setAdcCost(0)
                            .setFinalCost(0)
                            .build())
                    .toList();
            log.info("{}", String.format(STANDARD_FIELD_INFO, "Rates", rates));

            ArtifactBody artifactBody = ArtifactBody.newBuilder()
                    .setRouteRules(shipOptions)
                    .setRateShop(RateShop.newBuilder()
                            .setRates(rates)
                            .build())
                    .setTransitTimes(transitTimesList)
                    .build();

            ShipmentArtifact routeRuleArtifact = ShipmentArtifact.newBuilder()
                    .setArtifactHeader(artifactHeader)
                    .setArtifactBody(artifactBody)
                    .build();
            log.info(String.format(STANDARD_FIELD_INFO, "ShipmentArtifact-RouteRules", routeRuleArtifact));

            // Send route rules artifact
            artifactProducer.sendPayload(routeRuleArtifact);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private static List<Processes> buildProcessDetails(boolean isErrorOccurred,
                                                       RouteRules routeRules) {
        String msg = "RouteRuleArtifactService.buildProcessDetails()";
        List<Processes> processes = new ArrayList<>();
        try {
            Processes carrierProcess = Processes.newBuilder()
                    .setProcessCode(ROUTE_RULE_PROCESS_CODE)
                    .setProcessName(ROUTE_RULE_PROCESS_NAME)
                    .setProcessStatus(isErrorOccurred ? PROCESS_STATUS_FAILED : PROCESS_STATUS_COMPLETED)
                    .setStartTime(MDC.get(X_JOB_START_TIME) != null ? Long.valueOf(MDC.get(X_JOB_START_TIME)) : DateUtil.currentDateTimeInLong())
                    .setEndTime(DateUtil.currentDateTimeInLong())
                    .setOperationId(getCorrelationId())
                    .setProcessException(buildProcessException(routeRules))
                    .build();
            processes.add(carrierProcess);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return processes;
    }

    private static ProcessException buildProcessException(RouteRules routeRules) {
        StringBuilder code = new StringBuilder();
        StringBuilder description = new StringBuilder();
        StringBuilder source = new StringBuilder();
        // Add any exception
        if (null != routeRules && null != routeRules.getRulesError()) {
            routeRules.getRulesError().forEach(rulesError -> {
                code.append(formatExceptionDetail(rulesError, rulesError.getProcessException().getCode()));
                description.append(formatExceptionDetail(rulesError, rulesError.getProcessException().getDescription()));
                source.append(formatExceptionDetail(rulesError, rulesError.getProcessException().getSource()));
            });
        }

        return ProcessException.newBuilder()
                .setCode(code)
                .setDescription(description)
                .setSource(source)
                .build();
    }

    private static String formatExceptionDetail(RulesError error, CharSequence detail) {
        return String.format(PROCESS_EXCEPTION_DETAIL, detail, error.getTargetCarrierCode(), error.getTargetCarrierModeCode());
    }

    public void buildAndSendErrorRouteRuleArtifact(ShipmentInfo shipmentInfo,
                                                   String messageCorrelationId,
                                                   String artifactType,
                                                   String errorCode,
                                                   String errorMessage,
                                                   String errorSource) {
        buildAndSendErrorRouteRuleArtifact(
                shipmentInfo, messageCorrelationId, artifactType, buildRouteRulesWithError(errorCode, errorMessage, errorSource));
    }

    public void buildAndSendErrorRouteRuleArtifact(ShipmentInfo shipmentInfo,
                                                   String messageCorrelationId,
                                                   String artifactType,
                                                   RouteRules routeRules) {
        String msg = "RouteRuleArtifactService.buildAndSendErrorLabelArtifact()";
        try {

            ArtifactHeader artifactHeader = ArtifactHeader.newBuilder()
                    .setArtifactCreationDay(DateUtil.currentDateTimeInLong())
                    .setArtifactType(artifactType)
                    .setArtifactStatus(BUILD_ARTIFACT_STATUS_ERROR)
                    .setShipmentArtifactId(getCorrelationId())
                    .setMessageCorrelationId(!StringUtils.isEmpty(messageCorrelationId)
                            ? messageCorrelationId : STRING_EMPTY)
                    .setShipmentCorrelationId(!ObjectUtils.isEmpty(shipmentInfo)
                            && !StringUtils.isEmpty(shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                            ? shipmentInfo.getShipmentHeader().getShipmentCorrelationId() : STRING_EMPTY)
                    .setProcesses(buildProcessDetails(true,routeRules)).build();

            ShipmentArtifact routeRulesArtifact = ShipmentArtifact.newBuilder().setArtifactHeader(artifactHeader)
                    .setArtifactBody(ArtifactBody.newBuilder().setRouteRules(routeRules).build()).build();

            log.info(String.format(STANDARD_FIELD_INFO, "RouteRulesArtifactError", routeRulesArtifact));

            // Send Shipment Label Artifact
            artifactProducer.sendPayload(routeRulesArtifact);

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    public static Map<String, RouteRules> getMapOfRouteRulesByCarrier(
            Set<String> carrierCodes, ShipmentInfo shipmentInfo, List<BusinessRouteRulesEntity> routeRulesEntities) {
        Map<String, RouteRules> routeRulesByCarrier = new HashMap<>();
        carrierCodes.forEach(carrierCode -> {
            RouteRules routeRules = buildEmptyRouteRules();
            routeRules.setRulesInclude(buildRulesIncludeForCarrier(shipmentInfo, routeRulesEntities, carrierCode));
            routeRulesByCarrier.put(carrierCode, routeRules);
        });
        return routeRulesByCarrier;
    }

    public static List<RulesInclude> buildRulesIncludeForCarrier(
            ShipmentInfo shipmentInfo, List<BusinessRouteRulesEntity> routeRulesEntities, String carrierCode) {
        ShipmentDatesInfo transitInfo = shipmentInfo.getTransitDetails().getDateDetails();
        return routeRulesEntities.stream()
                .filter(routeRulesEntity -> StringUtils.equalsIgnoreCase(carrierCode, routeRulesEntity.getTargetCarrierCode()))
                .map(routeRulesEntity -> RulesInclude.newBuilder()
                        .setRouteRuleId(String.valueOf(routeRulesEntity.getRouteRuleId()))
                        .setRuleCode(routeRulesEntity.getRuleCode())
                        .setRuleType(routeRulesEntity.getRuleType())
                        .setApplyType(routeRulesEntity.getApplyType())
                        .setName(routeRulesEntity.getName())
                        .setDescription(routeRulesEntity.getDescription())
                        .setResponseNotes(STRING_EMPTY)
                        .setPriority(routeRulesEntity.getPriority())
                        .setTargetCarrierCode(routeRulesEntity.getTargetCarrierCode())
                        .setTargetCarrierModeCode(routeRulesEntity.getTargetCarrierModeCode())
                        .setApplicables(buildApplicablesList(routeRulesEntity))
                        .setPlannedShipDate(transitInfo.getPlannedShipDate())
                        .setPlannedDeliveryDate(transitInfo.getPlannedDeliveryDate())
                        .build())
                .toList();
    }

    public static RouteRules buildEmptyRouteRules() {
        return RouteRules.newBuilder()
                .setRulesOveride(new ArrayList<>())
                .setRulesInclude(new ArrayList<>())
                .setRulesExclude(new ArrayList<>())
                .setRulesError(new ArrayList<>())
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

    public static List<Applicables> buildApplicablesList(BusinessRouteRulesEntity routeRulesEntity) {
        List<Applicables> listOfApplicables = new ArrayList<>();
        listOfApplicables.add(buildApplicables("Ship Via", routeRulesEntity.getShipvia(), routeRulesEntity.getApplyType()));
        listOfApplicables.add(buildApplicables("Order Type", routeRulesEntity.getOrderType(), routeRulesEntity.getApplyType()));
        listOfApplicables.add(buildApplicables("Source Location", routeRulesEntity.getSrcLocation(), routeRulesEntity.getApplyType()));
        listOfApplicables.add(buildApplicables("Destination Location", routeRulesEntity.getDstLocation(), routeRulesEntity.getApplyType()));

        return listOfApplicables;
    }

    public static Applicables buildApplicables(String ruleGroup, Object challengeName, String applyType) {
        String challengeNameStr = !ObjectUtils.isEmpty(challengeName) ? new Gson().toJson(challengeName) : STRING_EMPTY;
        if (challengeNameStr.length() > 220) {
            challengeNameStr = challengeNameStr.substring(0, 220).concat(ELLIPSIS);
        }
        return Applicables.newBuilder()
                .setApplyType(applyType)
                .setRuleGroup(ruleGroup)
                .setChallengeName(challengeNameStr)
                .setPositiveMatch(true)
                .build();
    }

    private RuleResult buildRuleResult(RulesInclude routeRulesEntity) {
        return RuleResult.newBuilder()
                .setId(String.valueOf(routeRulesEntity.getRouteRuleId()))
                .setCode(routeRulesEntity.getRuleCode())
                .setType(routeRulesEntity.getRuleType())
                .setTargetCarrierCode(routeRulesEntity.getTargetCarrierCode())
                .setTargetCarrierModeCode(routeRulesEntity.getTargetCarrierModeCode())
                .setResponseNotes(STRING_EMPTY)
                .setApplicables(new ArrayList<>()).build();
    }

    public void buildAndPublishRouteRules(
            ShipmentInfo shipmentInfo, RouteRules shipOptions, List<RateshopRatesDTO> rateShopRates, String messageCorrelationId) {
        String msg = "BusinessRouteRulesService.buildAndPublishRouteRules()";
        try {
            buildAndSendRouteRuleArtifact(shipmentInfo,
                    shipOptions, rateShopRates,
                    messageCorrelationId);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            buildAndSendErrorRouteRuleArtifact(shipmentInfo,
                    messageCorrelationId,
                    BUILD_ARTIFACT_ROUTE_RULE,
                    CODE_NO_ARTIFACTS,
                    String.format(STANDARD_EXCEPTION_MESSAGE, e.getMessage(), ExceptionUtils.getStackTrace(e)),
                    CODE_NO_ARTIFACTS_SOURCE);
        }
    }
}
