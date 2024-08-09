package com.luluroute.ms.routerules.entity;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ServiceAttributes implements Serializable{


	@JsonProperty("applytype")
	private String applyType;

    @JsonProperty("isintl")
	private String[] isINTL;
	
    @JsonProperty("isrural")
	private String[] isRural;
   
    @JsonProperty("ispobox")
	private String[] isPOBox;

    @JsonProperty("ismilitary")
	private String[] isMilitary;
	
    @JsonProperty("isxbordereligible")
	private String[] isXborderEligible;
	
    @JsonProperty("isemployee")
	private String[] isEmployee;

    @JsonProperty("discountedorder")
	private String[] discountedOrder;

    @JsonProperty("isloyalty")
	private String[] isLoyalty;

}
