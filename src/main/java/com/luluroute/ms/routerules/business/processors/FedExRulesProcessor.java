package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.avro.shipment.shared.DimensionItem;
import com.logistics.luluroute.avro.shipment.shared.LocationItem;
import com.logistics.luluroute.carrier.fedex.rateshop.entity.FedexRateshopZoneCache;
import com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopHelper;
import com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopRedisService;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_DATA_RATE_SOURCE;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_RATE;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.eliminateAllShipOptionsForError;
import static com.luluroute.ms.routerules.business.util.Constants.*;

@Slf4j
@Component
public class FedExRulesProcessor extends AbstractRulesProcessor {

    @Value("${config.serviceurl.fedex.transit-time}")
    private String transitTimeUrl;
    @Value("${config.serviceurl.fedex.rateshop}")
    private String carrierRateShopUrl;
    @Autowired
    private FedexRateshopRedisService fedexRateshopRedisService;
    private static final String CARRIER_CODE = FEDEX;


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
        log.debug("FedExRulesProcessor.loadRates()");
        List<RateshopRatesDTO> rateshopRates = List.of();
        String rateShopRateKey = "";
        try {
            if(shipOptions.getRulesInclude().isEmpty()) {
                return rateshopRates;
            }

            RateShopCarrierInput rateShopCarrierInput = buildRateShopCarrierInput(shipmentInfo);

            FedexRateshopZoneCache rateshopZoneCache = fedexRateshopRedisService.checkCacheForRateshopZone(rateShopCarrierInput);
            log.debug(String.format(STANDARD_FIELD_INFO, "Rateshop Zone # ", rateshopZoneCache));

            if(null != rateshopZoneCache) {
                log.info(String.format(STANDARD_FIELD_INFO, "Rateshop Zones # ", rateshopZoneCache.getRateshopZone()));
                rateShopRateKey = FedexRateshopHelper.getRateShopRatesKey(rateShopCarrierInput, rateshopZoneCache);
            }

            log.info(String.format(STANDARD_FIELD_INFO, "RateShopRateKey #", rateShopRateKey));
            rateshopRates = redisCacheLoader.getRateForCarrier(
                    rateShopRateKey, rateShopCarrierInput, carrierRateShopUrl, CARRIER_CODE, shipOptions)
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

    private RateShopCarrierInput buildRateShopCarrierInput(ShipmentInfo shipmentInfo) {
        LocationItem addressFrom = shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom();
        LocationItem addressTo = shipmentInfo.getShipmentHeader().getDestination().getAddressTo();
        DimensionItem dimensionDetails = shipmentInfo.getShipmentPieces().get(0).getDimensionDetails();
        return RateShopCarrierInput.builder()
                .originZipCode(formatToFiveDigitZipCode(addressFrom, isZipCodeFormatEnabled, zipCodeFormatCountries))
                .originCountry(String.valueOf(addressFrom.getCountry()))
                .destinationZipCode(formatToFiveDigitZipCode(addressTo, isZipCodeFormatEnabled, zipCodeFormatCountries))
                .destinationCountry(String.valueOf(addressTo.getCountry()))
                .weight(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue())
                .width(dimensionDetails.getWidth())
                .height(dimensionDetails.getHeight())
                .length(dimensionDetails.getLength())
                .isMilitary(shipmentInfo.getOrderDetails().getIsMilitary())
                .build();
    }
}

