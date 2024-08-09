package com.luluroute.ms.routerules.business.dto;

import com.logistics.luluroute.domain.Shipment.Service.ShipmentDatesInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransitResponse {
    private String orderType;
    private String carrierMode;
    private String timeZone;
    private ShipmentDatesInfo shipmentDatesInfo;
}
