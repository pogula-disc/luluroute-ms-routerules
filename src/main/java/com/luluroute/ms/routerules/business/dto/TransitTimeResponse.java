package com.luluroute.ms.routerules.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TransitTimeResponse {
    private long responseDeliveryDate;
    private String carrierMode;
    private String failureMessage;
    
    //changes for VEHO
    private Double rate;
    private int transitDays;
}
