package com.luluroute.ms.routerules.business.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DstPrimaryEntityCode {

    @JsonProperty("applytype")
    private String applyType;
    @JsonProperty("primaryentity")
    private String[] primaryEntity;

}
