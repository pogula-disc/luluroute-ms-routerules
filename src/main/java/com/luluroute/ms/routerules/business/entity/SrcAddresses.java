package com.luluroute.ms.routerules.business.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class SrcAddresses {

    private UUID id;
    private String applyType;
    private String dst1;
    private String dst2;
    private String dst3;
    private String country;
    private int zip;
    private String state;
    private String city;
}
