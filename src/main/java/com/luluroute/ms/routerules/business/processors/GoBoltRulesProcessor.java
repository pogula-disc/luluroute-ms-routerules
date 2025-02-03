package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.luluroute.ms.routerules.business.util.Constants.GOBOLT;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_KEY;


@Slf4j
@Component
public class GoBoltRulesProcessor extends AbstractRulesProcessor {

    private static final String CARRIER_CODE = Constants.GOBOLT;

    @Value("${config.serviceurl.gobolt.transit-time}")
    private String transitTimeUrl;

    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile)
            throws MappingFormatException, ShipmentMessageException {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);
        List<TransitTimeRequest> validTransitTimeRequests = super.getTransitTimeRequests(shipOptions, shipmentInfo,
                entityProfile);
        List<TransitTimeResponse> transitTimeResponses = super.callCarrierTransitTime(validTransitTimeRequests,
                transitTimeUrl);
        super.updatePlannedDeliveryDateForShipOptions(shipOptions, shipmentInfo, transitTimeResponses);
    }

    @Override
    public List<RateshopRatesDTO> loadRates(RouteRules shipOptions, ShipmentInfo shipmentInfo) {
        log.debug("GOBOLTRulesProcessor.loadRates()");
        String key = String.format(RATE_KEY, GOBOLT,
                shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode(),
                shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode(),
                Math.ceil(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue()));

        return redisCacheLoader.getRateForCarrier(key, shipOptions.getRulesInclude().get(0).getTargetCarrierCode().toString());
    }

}