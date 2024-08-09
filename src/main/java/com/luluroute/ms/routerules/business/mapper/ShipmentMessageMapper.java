package com.luluroute.ms.routerules.business.mapper;

import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.util.Constants;
import com.luluroute.ms.routerules.business.util.DateUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@Mapper(uses = {CommonMapper.class})
public interface ShipmentMessageMapper {


    ShipmentMessageMapper INSTANCE = Mappers.getMapper(ShipmentMessageMapper.class);

    /**
     * Use Mapstruct to do the mapping of ShipmentUnit to FactoryASNShipUnit inoder to persist in database.
     *
     * @param shipmentMessage
     * @return
     * @throws MappingFormatException
     */
    @Mapping(source = "shipmentHeader", target = "shipmentHeader")
    @Mapping(source = "transitDetails", target = "transitDetails")
    @Mapping(source = "orderDetails", target = "orderDetails")
    @Mapping(source = "shipmentStatus", target = "shipmentStatus")
    @Mapping(source = "shipmentPieces", target = "shipmentPieces")
    public com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo mapShipmentMessage(ShipmentInfo shipmentMessage)
            throws MappingFormatException;

    default int toInt() {
        return -1;
    }

    /**
     * Custom mapping to transform the date
     *
     * @param strDate
     * @return
     * @throws MappingFormatException
     */
    default Date convertToDate(CharSequence strDate) throws MappingFormatException {
        if (!ObjectUtils.isEmpty(strDate))
            return DateUtil.convertToDate(strDate.toString(), null);
        return Constants.NULL;
    }
}
