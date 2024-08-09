package com.luluroute.ms.routerules.business.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentDatesInfo;
import com.logistics.luluroute.domain.artifact.message.ShipmentArtifact;
import com.luluroute.ms.routerules.business.config.SwaggerConfig;
import com.luluroute.ms.routerules.business.dto.*;
import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;
import com.luluroute.ms.routerules.business.service.BusinessRulesMaintenanceService;
import com.luluroute.ms.routerules.business.util.Constants;
import com.luluroute.ms.routerules.business.util.ShipmentUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.luluroute.ms.routerules.business.mapper.BusinessSearchRestToMessageMapper.mapSearchResponseFromShipmentArtifact;
import static com.luluroute.ms.routerules.business.mapper.BusinessSearchRestToMessageMapper.mapShipOptionsResponseFromArtifactSuccess;
import static com.luluroute.ms.routerules.business.util.Constants.X_CORRELATION_ID;
import static com.luluroute.ms.routerules.business.util.Constants.X_TRANSACTION_REFERENCE;

@RestController
@Slf4j
@RequestMapping("v1/api/business")
@Api(value = "/v1/api/business", tags = { SwaggerConfig.ROUTERULES_SVC_TAG })
@CrossOrigin()
public class BusinessRuleMaintenanceController {

    @Autowired
    private BusinessRulesMaintenanceService businessRulesMaintenanceService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Creates and Saves Route Rule
     * @param routeRulesDto Route Rule object to be created
     * @return Created Route Rule
     */
    @PostMapping(value = "/rule", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RouteRulesManageResponse> createRouteRule(@RequestBody RouteRulesDto routeRulesDto) {
        try {
            MDC.put(X_CORRELATION_ID, ShipmentUtils.getCorrelationId());
            BusinessRouteRulesEntity createdRule = businessRulesMaintenanceService.createRule(routeRulesDto);
            RouteRulesManageResponse response = buildRouteRulesManageResponse(createdRule, HttpStatus.CREATED, "Route Rule Successfully Created");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } finally {
            MDC.remove(X_CORRELATION_ID);
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    /**
     * Finds and Updates Existing Route Rule
     * @param routeRulesDto Route Rule object to be updated
     * @param routeRuleId unique identifier for rule that needs updating
     * @return Updated Route Rule
     */
    @PutMapping(value = "/rule/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RouteRulesManageResponse> updateRouteRule(@PathVariable(value="id") UUID routeRuleId, @RequestBody RouteRulesDto routeRulesDto) {
        try {
            MDC.put(X_CORRELATION_ID, ShipmentUtils.getCorrelationId());
            BusinessRouteRulesEntity updatedRule = businessRulesMaintenanceService.updateRule(routeRuleId, routeRulesDto);
            RouteRulesManageResponse response = buildRouteRulesManageResponse(updatedRule, HttpStatus.OK, "Route Rule Successfully Updated");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } finally {
            MDC.remove(X_CORRELATION_ID);
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    /**
     * Deactivates Existing Route Rule
     * @param updatedBy optional header value to indicate who has updated the route rule last
     * @param routeRuleId unique identifier for rule that needs updating
     * @return Deactivated Route Rule
     */
    @DeleteMapping(value = "/rule/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RouteRulesManageResponse> deactivateRouteRule(@RequestHeader("Updated-By") String updatedBy, @PathVariable(value="id") UUID routeRuleId) {
        try {
            MDC.put(X_CORRELATION_ID, ShipmentUtils.getCorrelationId());
            BusinessRouteRulesEntity deactivatedRule = businessRulesMaintenanceService.deactivateRule(routeRuleId, updatedBy);
            RouteRulesManageResponse response = buildRouteRulesManageResponse(deactivatedRule, HttpStatus.OK, "Route Rule Successfully Deactivated");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } finally {
            MDC.remove(X_CORRELATION_ID);
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    /**
     * Search for list of route rules based on search params
     * @param searchRequest Request payload to search route rules
     * @return route rules that fit the requested search params
     */
    @PostMapping(value = "/rule/search", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> searchRouteRule(@RequestBody RouteRuleSearchRequest searchRequest) {
        try {
            MDC.put(X_CORRELATION_ID, ShipmentUtils.getCorrelationId());
            List<BusinessRouteRulesEntity> rules = businessRulesMaintenanceService.getRouteRuleSearch(searchRequest);
            return new ResponseEntity<>(rules, HttpStatus.OK);
        } finally {
            MDC.remove(X_CORRELATION_ID);
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    /**
     * Get the full shipment artifact, or just condensed details, for a full shipment message
     */
    @PostMapping(value = "/target-carrier/search-by-message", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> searchTargetCarrierByShipmentMessage(
            @RequestBody ShipmentMessage shipmentMessage,
            @RequestParam(required = false, defaultValue = "false") boolean includeFullBody,
            @RequestHeader(required = false, value = Constants.IS_MOCK_ENABLED) boolean isMockEnabled) {

        String messageCorrelationId = shipmentMessage.getMessageHeader().getMessageCorrelationId();
        messageCorrelationId = "RouteRulesTargetCarrierUtility-" + messageCorrelationId;
        shipmentMessage.getMessageHeader().setMessageCorrelationId(messageCorrelationId);
        MDC.put(X_CORRELATION_ID, messageCorrelationId);
        MDC.put(Constants.IS_MOCK_ENABLED, String.valueOf(isMockEnabled));

        try {
            ShipmentArtifact shipmentArtifact =
                    businessRulesMaintenanceService.processRouteRulesForFullShipmentMessage(shipmentMessage, messageCorrelationId);

            if(includeFullBody) {
                return new ResponseEntity<>(shipmentArtifact, HttpStatus.valueOf((int)shipmentArtifact.getArtifactHeader().getArtifactStatus()));
            }
            TargetCarrierSearchResponse targetCarrierSearchResponse =
                    mapSearchResponseFromShipmentArtifact(shipmentArtifact, shipmentMessage);
            return new ResponseEntity<>(targetCarrierSearchResponse, HttpStatus.valueOf((int)shipmentArtifact.getArtifactHeader().getArtifactStatus()));

        } finally {
            MDC.remove(X_CORRELATION_ID);
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    // TODO: To be refactored
    /**
     * Get the full shipment artifact, or just condensed details, for a full shipment message
     */
    @PostMapping(value = "/search-ship-options", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> searchShipOptions(
            @RequestBody TransitRequest transitRequest) {

        String resourceName = "shipment_request.json";
        String jsonString;
        ShipmentMessage shipmentMessage = null;
        try {
            jsonString = getJsonString(resourceName);
            shipmentMessage = new ObjectMapper().readValue(jsonString, com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String messageCorrelationId = UUID.randomUUID().toString();
        messageCorrelationId = "RouteRulesTargetCarrierUtility-" + messageCorrelationId;
        MDC.put(X_CORRELATION_ID, messageCorrelationId);


        try {
            businessRulesMaintenanceService.populateShipmentMessage(transitRequest, shipmentMessage);
            ShipmentArtifact shipmentArtifact =
                    businessRulesMaintenanceService.processRouteRulesForFullShipmentMessage(shipmentMessage, messageCorrelationId);

            ShipmentDatesInfo shipmentDatesInfo =
                    shipmentMessage.getMessageBody().getShipments().get(0).getTransitDetails().getDateDetails();
            ShipOptionsResponse ShipOptionsResponse =
                    mapShipOptionsResponseFromArtifactSuccess(shipmentArtifact, shipmentDatesInfo);
            return new ResponseEntity<>(ShipOptionsResponse, HttpStatus.valueOf((int)shipmentArtifact.getArtifactHeader().getArtifactStatus()));

        } finally {
            MDC.remove(X_CORRELATION_ID);
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    private static RouteRulesManageResponse buildRouteRulesManageResponse(BusinessRouteRulesEntity rule, HttpStatus status, String message) {
        return RouteRulesManageResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(rule)
                .build();
    }

    public String getJsonString(String resourceName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(resourceName)).getFile());
        return Files.readString(file.toPath());
    }

}
