package com.luluroute.ms.routerules.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransitRequest {
    private String orderType;
    private String originEntityCode;
    private String destinationEntityCode;
    private String carrierMode;
    private String originCountry;
    private String originState;
    private String originCity;
    private String originPostalCode;
    private Long plannedShipDate;
    private Long plannedDeliveryDate;
    private String destinationCountry;
    private String destinationState;
    private String destinationCity;
    private String destinationPostalCode;
    private boolean isSaturdayDelivery;
    private Double weight;
    private Double valueOfContent;
    private String currency;
    private boolean isMilitary;
    private boolean isResidential;
    private boolean isPOBox;
    private boolean hazmat;
}
