package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DstAddresses {

    private UUID id;
    private String applyType;
    private String dst1;
    private String dst2;
    private String dst3;
    private String country;
    private String zip;
    private String state;
    private String city;
}
