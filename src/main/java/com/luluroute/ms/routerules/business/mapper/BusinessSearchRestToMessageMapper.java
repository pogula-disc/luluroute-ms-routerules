package com.luluroute.ms.routerules.business.mapper;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.OrderInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentDatesInfo;
import com.logistics.luluroute.domain.Shipment.Service.TransitInfo;
import com.logistics.luluroute.domain.artifact.message.*;
import com.luluroute.ms.routerules.business.dto.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessSearchRestToMessageMapper {

    /**
     * Maps full shipment artifact, from processing the route rules, to simple search response.
     */
    public static TargetCarrierSearchResponse mapSearchResponseFromShipmentArtifact(
            ShipmentArtifact shipmentArtifact, ShipmentMessage shipmentMessage) {

        if(shipmentArtifact.getArtifactHeader().getArtifactStatus() == HttpStatus.OK.value()) {
            return mapSearchResponseFromShipmentArtifactSuccess(shipmentArtifact, shipmentMessage);
        }
        return mapSearchResponseFromShipmentArtifactError(shipmentArtifact, shipmentMessage);
    }

    public static TargetCarrierSearchResponse mapSearchResponseFromShipmentArtifactSuccess(
            ShipmentArtifact shipmentArtifact, ShipmentMessage shipmentMessage) {
        ArtifactBody artifactBody = shipmentArtifact.getArtifactBody();

        TargetCarrierSearchResponse targetCarrierSearchResponse = TargetCarrierSearchResponse.builder()
                .messageCorrelationId(shipmentArtifact.getArtifactHeader().getMessageCorrelationId())
                .build();
        addDatesEntered(targetCarrierSearchResponse, shipmentMessage);
        addRulesIncludes(targetCarrierSearchResponse, artifactBody.getRouteRules().getRulesInclude());
        addRulesError(targetCarrierSearchResponse, artifactBody.getRouteRules().getRulesError());

        return targetCarrierSearchResponse;
    }

    /**
     * Object mapper util breaks with Lombok on fields called "isX..." -- It doesn't know how to map the boolean
     * methods, eg isIsX
     */
    public static void addMissingRequiredBooleanFieldsToAvro(
            ShipmentMessage jsonShipmentMessage, com.logistics.luluroute.avro.shipment.message.ShipmentMessage avroShipmentMessage) {
        OrderInfo jsonOrderInfo = jsonShipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails();
        TransitInfo jsonTransitInfo = jsonShipmentMessage.getMessageBody().getShipments().get(0).getTransitDetails();

        avroShipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails().setIsHazMat(jsonOrderInfo.isHazMat);
        avroShipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails().setIsPOBox(jsonOrderInfo.isPOBox());
        avroShipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails().setIsMilitary(jsonOrderInfo.isMilitary());
        avroShipmentMessage.getMessageBody().getShipments().get(0).getTransitDetails().setIsResidential(jsonTransitInfo.isResidential);
    }

    public static TargetCarrierSearchResponse mapSearchResponseFromShipmentArtifactError(
            ShipmentArtifact shipmentArtifact, ShipmentMessage shipmentMessage) {
        ProcessException processException = shipmentArtifact.getArtifactHeader().getProcesses().get(0).getProcessException();

        TargetCarrierSearchResponse targetCarrierSearchResponse = TargetCarrierSearchResponse.builder()
                .errorSource(processException.getSource())
                .errorDescription(processException.getDescription())
                .build();
        addDatesEntered(targetCarrierSearchResponse, shipmentMessage);
        addRulesError(targetCarrierSearchResponse, shipmentArtifact.getArtifactBody().getRouteRules().getRulesError());
        return targetCarrierSearchResponse;
    }


    private static String getDatePretty(long date) {
        Instant dateInstant = Instant.ofEpochSecond(date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd yyyy")
                .withZone(ZoneId.of("America/Los_Angeles"));
        return formatter.format(dateInstant);
    }


    private static void addRulesIncludes(TargetCarrierSearchResponse targetCarrierSearchResponse, List<RulesInclude> rulesIncludes) {
        targetCarrierSearchResponse.setPrioritizedShipOptions(rulesIncludes.stream()
                .map(shipOption -> TargetCarrierSearchShipOptionResponse.builder()
                        .carrierMode(shipOption.getTargetCarrierCode() + " " + shipOption.getTargetCarrierModeCode())
                        .name(shipOption.getName())
                        .description(shipOption.getDescription())
                        .ruleCode(shipOption.getRuleCode())
                        .errorSource(null)
                        .errorDescription(null)
                        .inclusionDetails(
                                shipOption.getApplicables().stream().map(
                                                applicable -> TargetCarrierSearchRuleDetailResponse.builder()
                                                        .ruleGroup(applicable.getRuleGroup())
                                                        .challengeName(applicable.getChallengeName())
                                                        .build())
                                        .toList())
                        .priority(shipOption.getPriority())
                        .cost(shipOption.getBaseRate())
                        .plannedShipDateModified(shipOption.getPlannedShipDate())
                        .plannedDeliveryDateModified(shipOption.getPlannedDeliveryDate())
                        .plannedShipDatePacificTimeModified(getDatePretty(shipOption.plannedShipDate))
                        .plannedDeliveryDatePacificTimeModified(getDatePretty(shipOption.getPlannedDeliveryDate()))
                        .build())
                .toList());
    }

    private static void addRulesError(TargetCarrierSearchResponse targetCarrierSearchResponse, List<RulesError> rulesError) {
        targetCarrierSearchResponse.setErrors(rulesError.stream()
                .map(shipOption -> TargetCarrierSearchShipOptionResponse.builder()
                        .carrierMode(shipOption.getTargetCarrierCode() + " " + shipOption.getTargetCarrierModeCode())
                        .name(shipOption.getName())
                        .description(shipOption.getDescription())
                        .ruleCode(shipOption.getRuleCode())
                        .errorSource(shipOption.getProcessException().getSource())
                        .errorDescription(shipOption.getProcessException().getDescription())
                        .inclusionDetails(null)
                        .priority(shipOption.getPriority())
                        .cost(shipOption.getBaseRate())
                        .plannedShipDateModified(shipOption.getPlannedShipDate())
                        .plannedDeliveryDateModified(shipOption.getPlannedDeliveryDate())
                        .plannedShipDatePacificTimeModified(getDatePretty(shipOption.plannedShipDate))
                        .plannedDeliveryDatePacificTimeModified(getDatePretty(shipOption.getPlannedDeliveryDate()))
                        .build())
                .toList());
    }


    private static void addDatesEntered(TargetCarrierSearchResponse targetCarrierSearchResponse, ShipmentMessage shipmentMessage) {
        com.logistics.luluroute.domain.Shipment.Service.ShipmentDatesInfo shipmentDatesInfoAsEntered =
                shipmentMessage.getMessageBody().getShipments().get(0).getTransitDetails().getDateDetails();

        targetCarrierSearchResponse.setPlannedShipDatePacificTimeEntered(getDatePretty(shipmentDatesInfoAsEntered.getPlannedShipDate()));
        targetCarrierSearchResponse.setPlannedDeliveryDatePacificTimeEntered(getDatePretty(shipmentDatesInfoAsEntered.getPlannedDeliveryDate()));
    }

    // TODO: To be refactored
    private static void addRulesIncludesToShipOptions(ShipOptionsResponse shipOptionsResponse, List<RulesInclude> rulesIncludes) {
        shipOptionsResponse.setPrioritizedShipOptions(rulesIncludes.stream()
                .map(shipOption -> ShipOptionsResponseDetails.builder()
                        .carrierMode(shipOption.getTargetCarrierCode() + " " + shipOption.getTargetCarrierModeCode())
                        .name(shipOption.getName())
                        .processError(shipOption.getDescription())
                        .cost(shipOption.getBaseRate())
                        .plannedShipDateModified(shipOption.getPlannedShipDate())
                        .plannedDeliveryDateModified(shipOption.getPlannedDeliveryDate())
                        .plannedShipDatePacificTimeModified(getDatePretty(shipOption.plannedShipDate))
                        .plannedDeliveryDatePacificTimeModified(getDatePretty(shipOption.getPlannedDeliveryDate()))
                        .build())
                .toList());
    }

    private static void addRulesErrorToShipOptions(ShipOptionsResponse shipOptionsResponse, List<RulesError> rulesError) {
        shipOptionsResponse.setErrors(rulesError.stream()
                .map(shipOption -> ShipOptionsErrorDetails.builder()
                        .carrierMode(shipOption.getTargetCarrierCode() + " " + shipOption.getTargetCarrierModeCode())
                        .errorDescription(shipOption.getProcessException().getDescription())
                        .build())
                .toList());
    }


    public static ShipOptionsResponse mapShipOptionsResponseFromArtifactSuccess(
            ShipmentArtifact shipmentArtifact, ShipmentDatesInfo shipmentDatesInfo) {
        ArtifactBody artifactBody = shipmentArtifact.getArtifactBody();

        ShipOptionsResponse ShipOptionsResponse = com.luluroute.ms.routerules.business.dto.ShipOptionsResponse.builder().build();
        addRulesIncludesToShipOptions(ShipOptionsResponse, artifactBody.getRouteRules().getRulesInclude());
        addRulesErrorToShipOptions(ShipOptionsResponse, artifactBody.getRouteRules().getRulesError());

        return ShipOptionsResponse;
    }
}
