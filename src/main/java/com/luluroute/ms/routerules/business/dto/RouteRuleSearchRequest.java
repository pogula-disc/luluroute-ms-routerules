package com.luluroute.ms.routerules.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRuleSearchRequest {

    Set<String> targetCarrierCodes;
    int hazMat;
    String orderType;
    String srcPrimaryEntity;
    String shipVia;
    String laneName;
    String isPOBox;
    String isMilitary;
    String isResidential;
    String sourceCountry;
    String sourceState;
    String sourceCity;
    String sourceZip;
    String destinationCountry;
    String destinationState;
    String destinationCity;
    String destinationZip;
    double weight;
    Double declaredValue;
    

}
