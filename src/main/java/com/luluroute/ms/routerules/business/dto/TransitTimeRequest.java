package com.luluroute.ms.routerules.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransitTimeRequest {
    private String orderType;
    private String originEntityCode;
    private String destinationEntityCode;
    private String carrierMode;
    private String originCountry;
    private String originPostalCode;
    private Long plannedShipDate;
    private String destinationCountry;
    private String destinationPostalCode;
    private boolean isSaturdayDelivery;
    private String timeZone;
    private int defaultTransitDays;
    private Double weight;
    private String weightUOM;
    private Double valueOfContent;
    private String currency;
    private String originState;
    private String destinationState;
}
