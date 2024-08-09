package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.artifact.message.RulesInclude;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper;
import com.luluroute.ms.routerules.business.repository.BusinessRouteRulesRepository;
import com.luluroute.ms.routerules.business.service.BusinessRuleArtifactService;
import com.luluroute.ms.routerules.business.service.RedisRehydrateService;
import com.luluroute.ms.routerules.rule.ShipmentRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.*;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.condenseRulesForAllCarriers;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.eliminateAllShipOptionsForError;
import static com.luluroute.ms.routerules.business.service.BusinessRuleArtifactService.getMapOfRouteRulesByCarrier;
import static com.luluroute.ms.routerules.business.util.Constants.*;
import static com.luluroute.ms.routerules.business.util.ShipmentUtils.getStringValue;
import static com.luluroute.ms.routerules.business.util.ShipmentUtils.retry;

@Slf4j
@Component
public class BusinessRulesSelector {

    @Autowired
    private BusinessRouteRulesRepository brRepository;
    @Autowired
    private RedisRehydrateService redisRehyderateService;
    @Autowired
    private BusinessRuleArtifactService artifactService;
    @Autowired
    private FedExRulesProcessor fedExRulesProcessor;
    @Autowired
    private DescartesRulesProcessor descartesRulesProcessor;
    @Autowired
    private GenericLTLRulesProcessor genericLTLRulesProcessor;
    @Autowired
    private StarTrackRulesProcessor starTrackRulesProcessor;
    @Autowired
    private USPSRulesProcessor uspsRulesProcessor;
    @Autowired
    private TForceRetailRulesProcessor tforceRetailRulesProcessor;
    @Autowired
    private TForceECOMMRulesProcessor tforceECOMMRulesProcessor;
    @Autowired
    private UPSRulesProcessor upsRulesProcessor;
    @Autowired
    private GoBoltRulesProcessor goBoltRulesProcessor;
    @Autowired
    private LaserShipRulesProcessor laserShipRulesProcessor;
    @Autowired
    private CanadaPostRulesProcessor canadaPostRulesProcessor;
    @Autowired
    BusinessRuleHelper businessRuleHelper;


    @Value("${spring.redis.retryCacheAttempts}")
    private int retryAttempt;
    @Value("${spring.redis.retryCacheInterval}")
    private long retryInterval;

    @Value("${config.destinationEntityCode.retailOrderTypes}")
    private String retailOrderType;
    @Value("${config.destinationEntityCode.validate}")
    private boolean validateDestEntityCode;

    @Value("${config.feature.lane-override.enabled}")
    private boolean isLaneOverrideEnabled;
    @Value("${config.feature.lane-override.originEntity}")
    private Set<String> laneOverrideByOriginEntity;

    /**
     * 1. Pulls the basic rule from db, identify the single or multi carrier rules process
     * 2. Select the rule processor
     *
     * @param shipmentInfo
     * @param messageCorrelationId
     */
    public void processRouteRules(ShipmentInfo shipmentInfo, String messageCorrelationId) {
        String msg = "BusinessRulesSelector.processRouteRules()";
        try {

            ShipmentRule.validateShipmentCorrelationId(shipmentInfo);
            String shipmentCorrelationId = getStringValue(shipmentInfo.getShipmentHeader().getShipmentCorrelationId());
            MDC.put(X_SHIPMENT_CORRELATION_ID, shipmentCorrelationId);
            String primaryEntityCode = MDC.get(X_PRIMARY_SOURCE_ENTITY);

            // Get the entity profile
            String originEntityCode = getStringValue(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode());
            String destinationEntityCode = getStringValue(shipmentInfo.getShipmentHeader().getDestination().getEntityCode());

            EntityPayload originEntityProfile = loadEntityProfile(originEntityCode);

            if (validateDestEntityCode && retailOrderType.contains(String.valueOf(shipmentInfo.getOrderDetails().getOrderType())))
                loadEntityProfile(destinationEntityCode);

            // Lanes are overridden for SFS request when requested carrier code and mode is provided
            // override can be configured for specific origin
            overrideLanesForRequestedCarrierNMode(shipmentInfo, primaryEntityCode);

            Set<String> assignedCarriers = new HashSet<>();
            originEntityProfile.getAssignedTransitModes().forEach(assignedTransitModes -> assignedCarriers.add(assignedTransitModes.getCarrierCode()));

            log.info("Rule {} Query Params # IsHazMat#{} getOrderType#{} primarySourceEntity#{} getShipVia#{} getLaneName#{} getIsPOBox#{} getIsMilitary#{} getIsResidential#{} getCountry#{} getState#{} getCity#{} getZipCode#{} getCountry#{} getState#{} getCity#{} getZipCode#{} getWeightDetails#{}", assignedCarriers,
                    shipmentInfo.getOrderDetails().getIsHazMat() ? 1 : 0,
                    shipmentInfo.getOrderDetails().getOrderType(),
                    primaryEntityCode,
                    shipmentInfo.getOrderDetails().getShipVia(),
                    shipmentInfo.getOrderDetails().getLaneName(),
                    shipmentInfo.getOrderDetails().getIsPOBox() ? 1 : 0,
                    shipmentInfo.getOrderDetails().getIsMilitary() ? 1 : 0,
                    shipmentInfo.getTransitDetails().getIsResidential() ? 1 : 0,
                    shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry(),
                    shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getState(),
                    shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCity(),
                    shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode(),
                    shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry(),
                    shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getState(),
                    shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCity(),
                    shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode(),
                    shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue());

            // Execute the basic rule filter from db
            List<BusinessRouteRulesEntity> routeRulesEntities = brRepository.loadApplicablesRules(assignedCarriers,
                    shipmentInfo.getOrderDetails().getIsHazMat() ? 1 : 0,
                    String.valueOf(shipmentInfo.getOrderDetails().getOrderType()),
                    primaryEntityCode,
                    String.valueOf(shipmentInfo.getOrderDetails().getShipVia()),
                    String.valueOf(shipmentInfo.getOrderDetails().getLaneName()),
                    String.valueOf(shipmentInfo.getOrderDetails().getIsPOBox() ? 1 : 0),
                    String.valueOf(shipmentInfo.getOrderDetails().getIsMilitary() ? 1 : 0),
                    String.valueOf(shipmentInfo.getTransitDetails().getIsResidential() ? 1 : 0),
                    String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getState()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCity()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getState()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCity()),
                    String.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode()),
                    shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue());

            if (CollectionUtils.isEmpty(routeRulesEntities))
                throw new ShipmentMessageException(CODE_PROFILE_DB, ROUTE_RULES_NOT_FOUND, CODE_NO_DATA_DB_SOURCE);

            log.info(String.format(STANDARD_FIELD_INFO, "Nbr Rules Selected #", routeRulesEntities.size()));
            log.debug(String.format(STANDARD_FIELD_INFO, "Rules Selected #", routeRulesEntities));

            // Get all the carrierCodes to eliminate the duplicates
            Set<String> carrierCodes = new HashSet<>();

            routeRulesEntities.forEach(routeRulesEntity -> carrierCodes.add(routeRulesEntity.getTargetCarrierCode()));
            log.debug(String.format(STANDARD_FIELD_INFO, "CARRIER Ship Options # ", carrierCodes));
            Map<String, RouteRules> routeRulesByCarrier = getMapOfRouteRulesByCarrier(carrierCodes, shipmentInfo, routeRulesEntities);

            prioritizeShipOptionsAndPublish(routeRulesByCarrier, shipmentInfo, originEntityProfile, messageCorrelationId);

        } catch (ShipmentMessageException e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            businessRuleHelper.sendErrorArtifact(shipmentInfo, messageCorrelationId, e);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            businessRuleHelper.sendErrorArtifact(shipmentInfo, messageCorrelationId);
        }
    }


    protected void prioritizeShipOptionsAndPublish(Map<String, RouteRules> routeRulesByCarrier, ShipmentInfo shipmentInfo,
            EntityPayload originEntityProfile, String messageCorrelationId) {
        String msg = "BusinessRulesSelector.processRouteRules()";
        // Map all types of Carrier Code & Carrier Specific Rule Processor

        try {
            final RuleProcessorSelector ruleProcessorSelector = buildRuleProcessorSelector();

            log.debug("Updating PSD/PDD on each ship option, if applicable to carrier");
            updateShipOptionDatesParallel(routeRulesByCarrier, shipmentInfo, originEntityProfile, ruleProcessorSelector);

            log.debug("Updating rates for each ship option");
            List<RateshopRatesDTO> rateShopRates = updateShipOptionRatesParallel(routeRulesByCarrier, shipmentInfo, ruleProcessorSelector);

            RouteRules routeRules = condenseRulesForAllCarriers(routeRulesByCarrier);
            if(routeRules.getRulesInclude().isEmpty()) {
                log.error(STANDARD_ERROR, msg, "All possible ship options were eliminated for errors");
                businessRuleHelper.sendErrorArtifact(shipmentInfo, messageCorrelationId, routeRules);
                return;
            }

            routeRules.getRulesInclude().sort(Comparator.comparing(RulesInclude::getPriority)
                    .thenComparing(RulesInclude::getBaseRate));
            log.debug(String.format(STANDARD_FIELD_INFO, "Final prioritized ship options #", routeRules.getRulesInclude()));

            artifactService.buildAndPublishRouteRules(shipmentInfo, routeRules, rateShopRates, messageCorrelationId);

        } catch (ShipmentMessageException e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            businessRuleHelper.sendErrorArtifact(shipmentInfo, messageCorrelationId, e);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            businessRuleHelper.sendErrorArtifact(shipmentInfo, messageCorrelationId);
        }
    }


    /**
     * @param entityCode
     * @return
     * @throws ShipmentMessageException
     * @throws InterruptedException
     */
    private EntityPayload loadEntityProfile(String entityCode) throws ShipmentMessageException, InterruptedException {
       String msg="BusinessRulesSelector.loadEntityProfile()";
        try {
           EntityPayload entityProfile = retry(() -> redisRehyderateService.getEntityByCode(entityCode), retryAttempt, retryInterval);
           log.debug(String.format(STANDARD_FIELD_INFO, "RouteRules - Entity Profile", entityProfile));

           if (ObjectUtils.isEmpty(entityProfile))
               throw new ShipmentMessageException(CODE_PROFILE, String.format(ROUTE_RULES_ENTITY_NOT_FOUND, entityCode), CODE_PROFILE_SOURCE);

           return entityProfile;
       } catch (Exception exp){
           log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(exp));
           throw new ShipmentMessageException(CODE_PROFILE, String.format(ROUTE_RULES_ENTITY_NOT_FOUND, entityCode), CODE_PROFILE_SOURCE);
       }
    }

    /**
     * Lanes are overridden for SFS request when requested carrier code and mode is provided
     * @param shipmentInfo
     */
    private void overrideLanesForRequestedCarrierNMode(ShipmentInfo shipmentInfo, String primaryEntityCode) {
        if(isLaneOverrideEnabled &&
                laneOverrideByOriginEntity.contains(primaryEntityCode) &&
                !StringUtils.isEmpty(shipmentInfo.getOrderDetails().getRequestedCarrierCode()) &&
                !StringUtils.isEmpty(shipmentInfo.getOrderDetails().getRequestedShipMode())) {

            shipmentInfo.getOrderDetails().setLaneName(String.format(REQUESTED_CARRIER_CODE_AND_MODE,
                            shipmentInfo.getOrderDetails().getRequestedCarrierCode(),
                                        shipmentInfo.getOrderDetails().getRequestedShipMode()));

            log.info("Override lanes {} for primary entity {}",
                    shipmentInfo.getOrderDetails().getLaneName(), primaryEntityCode);
        }
    }

    /**
     * Add Rule processor
     * Any new Carrier will needs to added here
     *
     * @return
     */
    private RuleProcessorSelector buildRuleProcessorSelector() {
        return new RuleProcessorSelector(Map.ofEntries(
        		Map.entry(AUPOST_STARTRACK, starTrackRulesProcessor),
        		Map.entry(DESCARTES, descartesRulesProcessor),
        		Map.entry(GENERIC, genericLTLRulesProcessor),
        		Map.entry(FEDEX, fedExRulesProcessor),
        		Map.entry(USPS, uspsRulesProcessor),
        		Map.entry(TFORCE_RETAIL, tforceRetailRulesProcessor),
        		Map.entry(TFORCE_ECOMM, tforceECOMMRulesProcessor),
        		Map.entry(UPS, upsRulesProcessor),
        		Map.entry(GOBOLT, goBoltRulesProcessor),
        		Map.entry(CNP,canadaPostRulesProcessor),
        		Map.entry(LSRS, laserShipRulesProcessor))
        		);
    }

    private void updateShipOptionDatesParallel(
            Map<String, RouteRules> routeRulesByCarrier, ShipmentInfo shipmentInfo, EntityPayload originEntityProfile,
            RuleProcessorSelector ruleProcessorSelector) {

        ForkJoinPool customThreadPool = new ForkJoinPool(routeRulesByCarrier.keySet().size());
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        routeRulesByCarrier.keySet().stream().parallel().forEach(carrierCode -> {
            try {
                customThreadPool.submit(() ->
                        ruleProcessorSelector.updateShipOptionDates(
                                contextMap, carrierCode, routeRulesByCarrier.get(carrierCode), shipmentInfo, originEntityProfile)
                ).get();
            } catch (InterruptedException e) {
                eliminateAllShipOptionsForError(routeRulesByCarrier.get(carrierCode), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                eliminateAllShipOptionsForError(routeRulesByCarrier.get(carrierCode), e);
            }
        });
    }

    private List<RateshopRatesDTO> updateShipOptionRatesParallel(
            Map<String, RouteRules> routeRulesByCarrier, ShipmentInfo shipmentInfo,
            RuleProcessorSelector ruleProcessorSelector) throws ShipmentMessageException {

        List<RateshopRatesDTO> rateShopRates = new ArrayList<>();

        ForkJoinPool customThreadPool = new ForkJoinPool(routeRulesByCarrier.keySet().size());
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        routeRulesByCarrier.keySet().stream().parallel().forEach(carrierCode -> {
            try {
                rateShopRates.addAll(customThreadPool.submit(() ->
                                ruleProcessorSelector.updateShipOptionRates(
                                        contextMap, carrierCode, routeRulesByCarrier.get(carrierCode), shipmentInfo))
                        .get());
            } catch (InterruptedException e) {
                eliminateAllShipOptionsForError(routeRulesByCarrier.get(carrierCode), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                eliminateAllShipOptionsForError(routeRulesByCarrier.get(carrierCode),
                        new ShipmentMessageException(CODE_PROFILE_RATE, String.format(RATE_NOT_FOUND,
                                routeRulesByCarrier.keySet(), "all", e.getMessage()), CODE_NO_DATA_RATE_SOURCE));
            }
        });

        return rateShopRates;
    }
}

