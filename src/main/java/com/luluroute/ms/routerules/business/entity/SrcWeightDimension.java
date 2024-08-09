package com.luluroute.ms.routerules.business.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrcWeightDimension {

    @JsonProperty("max")
    private String max;
    @JsonProperty("min")
    private String min;
    @JsonProperty("uom")
    private String uom;
    @JsonProperty("type")
    private String type;
    @JsonProperty("applytype")
    private String applyType;
    @JsonProperty("rangeinclusive")
    private String rangeInclusive;
}
