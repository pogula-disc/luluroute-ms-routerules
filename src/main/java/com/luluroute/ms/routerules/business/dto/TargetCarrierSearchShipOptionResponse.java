package com.luluroute.ms.routerules.business.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class TargetCarrierSearchShipOptionResponse {

    String carrierMode;
    String name;
    String description;
    String ruleCode;
    String errorSource;
    String errorDescription;
    long priority;
    double cost;
    long plannedShipDateModified;
    long plannedDeliveryDateModified;
    String plannedShipDatePacificTimeModified;
    String plannedDeliveryDatePacificTimeModified;
    List<TargetCarrierSearchRuleDetailResponse> inclusionDetails;

}
