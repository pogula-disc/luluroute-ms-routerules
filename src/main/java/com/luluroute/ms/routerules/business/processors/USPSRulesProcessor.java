package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierHelper.getRateShopCacheWeight;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_DATA_RATE_SOURCE;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_RATE;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.eliminateAllShipOptionsForError;
import static com.luluroute.ms.routerules.business.util.Constants.*;

@Slf4j
@Component
public class USPSRulesProcessor extends AbstractRulesProcessor {

    @Value("${config.serviceurl.usps.rateshop}")
    private String carrierRateShopUrl;
    @Value("${config.serviceurl.usps.transit-time}")
    private String transitTimeUrl;
    private static final String CARRIER_CODE = USPS;


    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile)
            throws MappingFormatException, ShipmentMessageException {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);
        List<TransitTimeRequest> validTransitTimeRequests = super.getTransitTimeRequests(shipOptions, shipmentInfo, entityProfile);
        List<TransitTimeResponse> transitTimeResponses = super.callCarrierTransitTime(validTransitTimeRequests, transitTimeUrl);
        super.updatePlannedDeliveryDateForShipOptions(shipOptions, shipmentInfo, transitTimeResponses);
    }

    @Override
    public List<RateshopRatesDTO> loadRates(RouteRules shipOptions, ShipmentInfo shipmentInfo) {
        log.debug("USPSRulesProcessor.loadRates()");
        List<RateshopRatesDTO> rateshopRates = List.of();
        try {
            if(shipOptions.getRulesInclude().isEmpty()) {
                return rateshopRates;
            }

            String key = String.format(RATE_KEY, CARRIER_CODE,
                    shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode(),
                    shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode(),
                    getRateShopCacheWeight(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue()));

            List<String> configuredMode = shipOptions.getRulesInclude().stream()
                    .filter(rulesIncluded -> rulesIncluded.getTargetCarrierCode().equals(CARRIER_CODE))
                    .map(rulesMap -> String.valueOf(rulesMap.getTargetCarrierModeCode())).toList();

            RateShopCarrierInput rateShopInput = RateShopCarrierInput.builder()
                    .originCountry(String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry()))
                    .originZipCode(String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode()))
                    .destinationZipCode(String.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode()))
                    .destinationCountry(String.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry()))
                    .weight(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue())
                    .width(shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getWidth())
                    .height(shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getHeight())
                    .length(shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getLength())
                    .isMilitary(shipmentInfo.getOrderDetails().getIsMilitary())
                    .modeCodes(Set.copyOf(configuredMode))
                    .build();

            log.info(String.format(STANDARD_FIELD_INFO, "RateShopRateKey #", key));
            rateshopRates = redisCacheLoader.getRateForCarrier(key, rateShopInput, carrierRateShopUrl, CARRIER_CODE, shipOptions)
                            .stream()
                            .filter(rate -> updateRateForShipOption(CARRIER_CODE, rate, shipOptions))
                            .toList();
            log.debug(String.format(STANDARD_FIELD_INFO, "Rates # ", rateshopRates));

            eliminateShipOptionsWithoutRates(shipOptions);

        } catch (ShipmentMessageException e) {
            eliminateAllShipOptionsForError(shipOptions, e);
        } catch (Exception e) {
            eliminateAllShipOptionsForError(shipOptions, new ShipmentMessageException(CODE_PROFILE_RATE,
                    String.format(RATE_NOT_FOUND, CARRIER_CODE, "all", e.getMessage()), CODE_NO_DATA_RATE_SOURCE));
        }
        return rateshopRates;
    }

}