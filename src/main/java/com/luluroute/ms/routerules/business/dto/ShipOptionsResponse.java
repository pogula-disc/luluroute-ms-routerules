package com.luluroute.ms.routerules.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class ShipOptionsResponse {

    List<ShipOptionsResponseDetails> prioritizedShipOptions;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<ShipOptionsErrorDetails> errors;

}
