package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LimitCarrierUsageAmount implements Serializable {

    private static final long serialVersionUID = 1L;
    private UUID id;
    private String[] carrierCode;
    private String[] entityCode;
    private Double limitAmt;
    private String period;

}
