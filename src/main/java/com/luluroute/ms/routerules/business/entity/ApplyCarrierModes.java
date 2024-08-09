package com.luluroute.ms.routerules.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyCarrierModes {
    private String carrierCode;
    private String modeCode;
}
