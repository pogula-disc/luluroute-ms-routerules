package com.luluroute.ms.routerules.business.util;

import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;

import java.util.Comparator;

public class PrioritySorter implements Comparator<BusinessRouteRulesEntity> {

    @Override
    public int compare(BusinessRouteRulesEntity one, BusinessRouteRulesEntity another) {

        int returnVal = 0;

        if (one.getPriority() > another.getPriority()) {
            returnVal = 1;
        }
        return returnVal;
    }
}

