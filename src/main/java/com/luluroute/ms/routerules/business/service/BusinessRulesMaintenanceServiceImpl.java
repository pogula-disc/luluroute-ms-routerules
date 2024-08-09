package com.luluroute.ms.routerules.business.service;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Service.OrderInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentDatesInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.luluroute.ms.routerules.business.dto.RouteRuleSearchRequest;
import com.luluroute.ms.routerules.business.dto.RouteRulesDto;
import com.luluroute.ms.routerules.business.dto.TransitRequest;
import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;
import com.luluroute.ms.routerules.business.exceptions.RecordNotFoundException;
import com.luluroute.ms.routerules.business.processors.BusinessRulesSelector;
import com.luluroute.ms.routerules.business.repository.BusinessRouteRulesRepository;
import com.luluroute.ms.routerules.business.util.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.luluroute.ms.routerules.business.mapper.BusinessSearchRestToMessageMapper.addMissingRequiredBooleanFieldsToAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessRulesMaintenanceServiceImpl implements BusinessRulesMaintenanceService {

    private final BusinessRouteRulesRepository businessRouteRulesRepository;
    private final BusinessRulesSelector businessRulesSelector;

    @Value("${routerules.active}")
    private int active;

    @Value("${routerules.inactive}")
    private int inactive;

    @Override
    public BusinessRouteRulesEntity createRule(@Valid RouteRulesDto routeRulesDto) {
        BusinessRouteRulesEntity routeRule = ObjectMapperUtil.map(routeRulesDto, BusinessRouteRulesEntity.class);
        routeRule.setRouteRuleId(UUID.randomUUID());
        routeRule.setCreatedDate(Timestamp.from(Instant.now()));
        routeRule.setActive(active);
        businessRouteRulesRepository.save(routeRule);
        return routeRule;
    }

    @Override
    public BusinessRouteRulesEntity updateRule(UUID routeRuleId, @Valid RouteRulesDto routeRulesDto) {
        Optional<BusinessRouteRulesEntity> queryRouteRule = businessRouteRulesRepository.findByRouteRuleId(routeRuleId);
        if (queryRouteRule.isPresent()) {
            BusinessRouteRulesEntity existingRouteRule = queryRouteRule.get();
            BusinessRouteRulesEntity updatedRouteRule = ObjectMapperUtil.map(routeRulesDto, BusinessRouteRulesEntity.class);
            updatedRouteRule.setRouteRuleId(existingRouteRule.getRouteRuleId());
            updatedRouteRule.setCreatedBy(existingRouteRule.getCreatedBy());
            updatedRouteRule.setCreatedDate(existingRouteRule.getCreatedDate());
            updatedRouteRule.setUpdatedDate(Timestamp.from(Instant.now()));
            businessRouteRulesRepository.save(updatedRouteRule);
            return updatedRouteRule;
        } else {
            throw new RecordNotFoundException(String.format("Route Rule with Id, %s, was not found in database.", routeRuleId));
        }
    }

    @Override
    public BusinessRouteRulesEntity deactivateRule(UUID routeRuleId, String updatedBy) {
        Optional<BusinessRouteRulesEntity> queryRouteRule = businessRouteRulesRepository.findByActiveAndRouteRuleId(active, routeRuleId);
        if (queryRouteRule.isPresent()) {
            BusinessRouteRulesEntity existingRouteRule = queryRouteRule.get();
            existingRouteRule.setUpdatedDate(Timestamp.from(Instant.now()));
            existingRouteRule.setUpdatedBy(updatedBy);
            existingRouteRule.setActive(inactive);
            businessRouteRulesRepository.save(existingRouteRule);
            return existingRouteRule;
        } else {
            throw new RecordNotFoundException(String.format("Route Rule with Id, %s, was not found in database.", routeRuleId));
        }
    }

    @Override
    public List<BusinessRouteRulesEntity> getRouteRuleSearch(RouteRuleSearchRequest searchRequest) {
        List<BusinessRouteRulesEntity> routeRulesEntities = businessRouteRulesRepository
                .loadApplicablesRules(
                        searchRequest.getTargetCarrierCodes(),
                        searchRequest.getHazMat(),
                        searchRequest.getOrderType(),
                        searchRequest.getSrcPrimaryEntity(),
                        searchRequest.getShipVia(),
                        searchRequest.getLaneName(),
                        searchRequest.getIsPOBox(),
                        searchRequest.getIsMilitary(),
                        searchRequest.getIsResidential(),
                        searchRequest.getSourceCountry(),
                        searchRequest.getSourceState(),
                        searchRequest.getSourceCity(),
                        searchRequest.getSourceZip(),
                        searchRequest.getDestinationCountry(),
                        searchRequest.getDestinationState(),
                        searchRequest.getDestinationCity(),
                        searchRequest.getDestinationZip(),
                        searchRequest.getWeight());
        if (routeRulesEntities.isEmpty()) {
            throw new RecordNotFoundException(String.format("Route Rule with search request, %s, was not found in database.", searchRequest));
        } else {
            return routeRulesEntities;
        }
    }

    @Override
    public com.logistics.luluroute.domain.artifact.message.ShipmentArtifact processRouteRulesForFullShipmentMessage(
            com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage shipmentMessage, String messageCorrelationId) {

        ShipmentMessage avroShipmentMessage = ObjectMapperUtil.map(shipmentMessage, ShipmentMessage.class);
        avroShipmentMessage.getMessageHeader().setMessageCorrelationId(messageCorrelationId);
        // Object mapper util breaks with Lombok on fields called "isX..."
        // It doesn't know how to map the boolean methods, eg isIsX
        addMissingRequiredBooleanFieldsToAvro(shipmentMessage, avroShipmentMessage);

        ShipmentInfo shipmentInfo = avroShipmentMessage.getMessageBody().getShipments().get(0);

        log.debug("REST request attempting to process route rules for shipment message = {}", shipmentInfo);
        businessRulesSelector.processRouteRules(shipmentInfo, messageCorrelationId);

        // Added to request context at end of rule selection by ShipmentArtifactsProducer
        ShipmentArtifact shipmentArtifact = (ShipmentArtifact) RequestContextHolder.getRequestAttributes()
                .getAttribute("shipmentArtifact", RequestAttributes.SCOPE_REQUEST);
        if(shipmentArtifact == null) {
            throw new RuntimeException("Issue processing search, see logs");
        }

        return ObjectMapperUtil.map(shipmentArtifact, com.logistics.luluroute.domain.artifact.message.ShipmentArtifact.class);
    }

    // TODO: To be refactored
    public void populateShipmentMessage(TransitRequest transitRequest,
                         com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage shipmentMessage) {
        shipmentMessage.getMessageBody().getShipments().forEach(shipmentInfo -> {
            LocationItem origin = shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom();
            LocationItem dest = shipmentInfo.getShipmentHeader().getDestination().getAddressTo();
            OrderInfo orderInfo = shipmentInfo.getOrderDetails();
            ShipmentDatesInfo shipmentDatesInfo = shipmentInfo.getTransitDetails().getDateDetails();
            orderInfo.setOrderType(transitRequest.getOrderType());
            orderInfo.setMilitary(transitRequest.isMilitary());
            orderInfo.setPOBox(transitRequest.isPOBox());
            orderInfo.setHazMat(transitRequest.isHazmat());
            shipmentInfo.getShipmentPieces()
                    .forEach(spInfo -> spInfo.getWeightDetails().setValue(transitRequest.getWeight()));

            if (transitRequest.getPlannedShipDate() != null)
                shipmentDatesInfo.setPlannedShipDate(transitRequest.getPlannedShipDate());
            if (transitRequest.getPlannedDeliveryDate() != null)
                shipmentDatesInfo.setPlannedDeliveryDate(transitRequest.getPlannedDeliveryDate());

            origin.setCountry(transitRequest.getOriginCountry());
            origin.setState(transitRequest.getOriginState());
            origin.setCity(transitRequest.getOriginCity());
            origin.setZipCode(transitRequest.getOriginPostalCode());

            dest.setCountry(transitRequest.getDestinationCountry());
            dest.setState(transitRequest.getDestinationState());
            dest.setCity(transitRequest.getDestinationCity());
            dest.setZipCode(transitRequest.getDestinationPostalCode());


        });
    }
}
