package com.luluroute.ms.routerules.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class RateshopRatesDTO {
    public static class RateshopRatesDTOBuilder {} // For Lombok+Javadoc build error

    String modeCode;
    Double baseRate;
    String carrierCode;
}
