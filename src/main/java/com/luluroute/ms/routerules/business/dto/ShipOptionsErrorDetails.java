package com.luluroute.ms.routerules.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class ShipOptionsErrorDetails {

    String carrierMode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String errorDescription;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String processError;

}
