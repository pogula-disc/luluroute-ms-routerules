package com.luluroute.ms.routerules.business.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransitTimeRateCache {
    public String originEntityCode;
    public String destPostalCode;
    public int transitDays;
    public Double rate; 
}

