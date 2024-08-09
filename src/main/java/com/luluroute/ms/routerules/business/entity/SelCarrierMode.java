package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelCarrierMode implements Serializable {

    private static final long serialVersionUID = 1L;
    private UUID id;
    private com.luluroute.ms.routerules.entity.ApplyCarrierModes[] applyCarrierModes;


}
