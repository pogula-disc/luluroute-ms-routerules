package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrcPath {

    private UUID id;
    private String applyType;
    private String[] path;
}
