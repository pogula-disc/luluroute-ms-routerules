package com.luluroute.ms.routerules.business.config;

import com.logistics.luluroute.util.DateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HolidayServiceConfig {

    @Value("${config.carrier.holidayrange}")
    private int holidayRange;

    public LocalDate getStartDate(String timeZone){
       return DateUtil.localDateForTimezone(timeZone);
    }

    public LocalDate getEndDate(LocalDate startDate){
        return startDate.plusDays(holidayRange);
    }
}
