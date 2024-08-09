package com.luluroute.ms.routerules.business.service;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.routerules.business.dto.RouteRuleSearchRequest;
import com.luluroute.ms.routerules.business.dto.RouteRulesDto;
import com.luluroute.ms.routerules.business.dto.TransitRequest;
import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

public interface BusinessRulesMaintenanceService {

    BusinessRouteRulesEntity createRule(@Valid RouteRulesDto routeRulesDto);

    BusinessRouteRulesEntity updateRule(UUID routeRuleId, @Valid RouteRulesDto routeRulesDto);

    BusinessRouteRulesEntity deactivateRule(UUID routeRuleId, String updatedBy);

    List<BusinessRouteRulesEntity> getRouteRuleSearch(RouteRuleSearchRequest searchRequest);

    com.logistics.luluroute.domain.artifact.message.ShipmentArtifact processRouteRulesForFullShipmentMessage(
            ShipmentMessage shipmentMessage, String messageCorrelationId);

    // TODO: To be refactored
    void populateShipmentMessage(TransitRequest transitRequest,
                                        com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage shipmentMessage);

}
