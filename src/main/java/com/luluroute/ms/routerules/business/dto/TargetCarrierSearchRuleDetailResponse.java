package com.luluroute.ms.routerules.business.dto;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class TargetCarrierSearchRuleDetailResponse {

    String ruleGroup;
    String challengeName;

}
