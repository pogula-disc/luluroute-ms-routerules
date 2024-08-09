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
public class SrcLocation {

    @JsonProperty("applytype")
    private String applyType;
    @JsonProperty("country")
    private String[] country;
    @JsonProperty("state")
    private String[] state;
    @JsonProperty("city")
    private String[] city;
    @JsonProperty("zip")
    private String[] zip;
}
