package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrcDeclaredValue {
    @JsonProperty("max")
    private String max;
    @JsonProperty("min")
    private String min;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("applytype")
    private String applyType;
    @JsonProperty("rangeinclusive")
    private String rangeInclusive;

}
