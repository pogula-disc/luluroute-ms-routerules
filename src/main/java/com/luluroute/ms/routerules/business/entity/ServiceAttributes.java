package com.luluroute.ms.routerules.business.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ServiceAttributes implements Serializable {


    private static final long serialVersionUID = 1L;

    @JsonProperty("applytype")
    private String applyType;

    @JsonProperty("ispobox")
    private String[] isPOBox;

    @JsonProperty("ismilitary")
    private String[] isMilitary;

    @JsonProperty("isintl")
    private String[] isINTL;

    @JsonProperty("isrural")
    private String[] isRural;

    @JsonProperty("isxbordereligible")
    private String[] isXborderEligible;

    @JsonProperty("isemployee")
    private String[] isEmployee;

    @JsonProperty("discountedorder")
    private String[] discountedOrder;

    @JsonProperty("isloyalty")
    private String[] isLoyalty;

    @JsonProperty("isresidential")
    private String[] isResidential;

}
