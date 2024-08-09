package com.luluroute.ms.routerules.business.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OrderType {

    @JsonProperty("applytype")
    private String applyType;
    @JsonProperty("ordertype")
    private String[] orderTypes;
}

