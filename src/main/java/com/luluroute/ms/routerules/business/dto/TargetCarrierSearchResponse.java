package com.luluroute.ms.routerules.business.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class TargetCarrierSearchResponse {

    String messageCorrelationId;
    String errorSource;
    String errorDescription;
    String plannedShipDatePacificTimeEntered;
    String plannedDeliveryDatePacificTimeEntered;
    List<TargetCarrierSearchShipOptionResponse> prioritizedShipOptions;
    List<TargetCarrierSearchShipOptionResponse> errors;

}
