package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrcDeclaredValue {
    private UUID srcDeclaredValueId;
    private String applyType;
    private String operator;
    private double value;
    private String currency;

}
