package com.luluroute.ms.routerules.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApplicableWeekDayTime {


    private UUID id;
    private String applyType;
    private Long weekDayMask;
    private String fromTime;
    private String toTime;
}

