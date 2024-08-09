package com.luluroute.ms.routerules.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class ShipOptionsResponseDetails {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String message;
    String carrierMode;
    String name;
    String description;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String errorDescription;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String processError;
    double cost;
    long plannedShipDateModified;
    long plannedDeliveryDateModified;
    String plannedShipDatePacificTimeModified;
    String plannedDeliveryDatePacificTimeModified;

}
