package com.luluroute.ms.routerules.business.carrier.service;

import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.luluroute.ms.routerules.business.carrier.entity.EntityApplicableHoliday;
import com.luluroute.ms.routerules.business.carrier.entity.EntityHoliday;
import com.luluroute.ms.routerules.business.carrier.repository.EntityAppHolidayRepository;
import com.luluroute.ms.routerules.business.carrier.repository.EntityHolidayRepository;
import com.luluroute.ms.routerules.business.config.HolidayServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_ERROR;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.util.DateUtil.FROM_DATE_FORMAT;

@Service
@Slf4j
public class HolidayService {

    @Autowired
    private EntityAppHolidayRepository appHolidayRepository;
    @Autowired
    private EntityHolidayRepository holidayRepository;
    @Autowired
    HolidayServiceConfig holidayServiceConfig;

    /**
     * @param shipmentInfoAPI
     * @param carrierId
     * @param carrierMode
     * @param entityTimeZone
     * @return
     */
    public List<String> getHolidaySet( ShipmentInfo shipmentInfoAPI,
                                       String carrierId,
                                       String carrierMode,
                                       String entityTimeZone) {
        String msg = "ShipmentTransitTimeService.getHolidaySet()";
        SimpleDateFormat dateFormat = new SimpleDateFormat(FROM_DATE_FORMAT);
        try {
            List<EntityApplicableHoliday> applicableHolidayList = appHolidayRepository.findApplicableHolidays(
                    carrierId, carrierMode, String.valueOf(shipmentInfoAPI.getShipmentHeader().getOrigin().getAddressFrom().getCountry()),
                    String.valueOf(shipmentInfoAPI.getShipmentHeader().getOrigin().getAddressFrom().getState()),
                    String.valueOf(shipmentInfoAPI.getShipmentHeader().getOrigin().getAddressFrom().getCity()));

            List<String> holidayIds = new ArrayList<>();
            for (EntityApplicableHoliday applicableHoliday :
                    applicableHolidayList) {
                holidayIds.add(applicableHoliday.getHolidayId().toString());
            }
            log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime - holidayIds ", holidayIds.size()));

            LocalDate currentLocalDate = holidayServiceConfig.getStartDate(entityTimeZone);
            log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime - currentLocalDate", currentLocalDate));

            List<EntityHoliday> entityHolidays = holidayRepository.findHolidays(holidayIds, currentLocalDate, holidayServiceConfig.getEndDate(currentLocalDate));

            List<String> entityHolidaySet = new ArrayList<>();
            if (!CollectionUtils.isEmpty(entityHolidays))
                entityHolidays.forEach(entityHoliday -> {
                    entityHolidaySet.add(dateFormat.format(entityHoliday.getHolidayDate()));
                });
            // Get the holiday set from database
            return entityHolidaySet;
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }
}