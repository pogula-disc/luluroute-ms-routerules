package com.luluroute.ms.routerules.rule;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_DATA_ERROR;
import static com.luluroute.ms.routerules.business.util.ExceptionConstants.CODE_NO_DATA;
import static com.luluroute.ms.routerules.business.util.ExceptionConstants.CODE_NO_DATA_SOURCE;

@Component
@Slf4j
public class ShipmentRule {
    /**
     * Validate Shipment Message Mandatory Attribute
     *
     * @param message
     * @return
     */
    public static boolean validateShipmentMessage(ShipmentMessage message) throws ShipmentMessageException {
        String msg = "ShipmentRule.validateShipmentMessage()";
        // check shipmentCorrelationId
        if (StringUtils.isEmpty(message.getMessageHeader().getMessageCorrelationId()))
            throw new ShipmentMessageException(CODE_NO_DATA,
                    String.format(STANDARD_DATA_ERROR, msg, "MessageCorrelationId", "No Data"), CODE_NO_DATA_SOURCE);

        // check Shipments
        if (CollectionUtils.isEmpty(message.getMessageBody().getShipments()))
            throw new ShipmentMessageException(CODE_NO_DATA,
                    String.format(STANDARD_DATA_ERROR, msg, "Shipments", "No Data"), CODE_NO_DATA_SOURCE);

        return Boolean.TRUE;
    }

    /**
     * Validate Shipment Message Mandatory Attribute
     *
     * @param shipmentInfo
     * @return
     */
    public static boolean validateShipmentCorrelationId(com.logistics.luluroute.avro.shipment.service.ShipmentInfo shipmentInfo) throws ShipmentMessageException {
        String msg = "ShipmentRule.validateShipmentCorrelationId()";
        // check shipmentCorrelationId
        if (ObjectUtils.isEmpty(shipmentInfo.getShipmentHeader())
                || ObjectUtils.isEmpty(shipmentInfo.getShipmentHeader().getShipmentCorrelationId()))
            throw new ShipmentMessageException(CODE_NO_DATA, String.format(STANDARD_DATA_ERROR,
                    msg, "ShipmentCorrelationId", "No Data"), CODE_NO_DATA_SOURCE);
        return Boolean.TRUE;
    }
}
