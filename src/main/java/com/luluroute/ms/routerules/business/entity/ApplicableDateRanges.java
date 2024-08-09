package com.luluroute.ms.routerules.business.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicableDateRanges implements Serializable, Comparator<ApplicableDateRanges> {

    private static final long serialVersionUID = 1L;

    private UUID Id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String start;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String ends;

    @Override
    public int compare(ApplicableDateRanges o1, ApplicableDateRanges o2) {

        return o1.getStart().compareTo(o2.getStart());
    }

}

