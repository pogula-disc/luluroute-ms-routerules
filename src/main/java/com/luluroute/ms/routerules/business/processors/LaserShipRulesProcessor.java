package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.artifact.message.RulesInclude;
import com.logistics.luluroute.avro.shipment.service.OrderInfo;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.avro.shipment.service.TransitInfo;
import com.logistics.luluroute.avro.shipment.shared.DimensionItem;
import com.logistics.luluroute.avro.shipment.shared.LocationItem;
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
import static com.luluroute.ms.routerules.business.util.Constants.LASERSHIP;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_KEY;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_NOT_FOUND;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.util.DateUtil.epochToIso8601String;

@Slf4j
@Component
public class LaserShipRulesProcessor extends AbstractRulesProcessor {

    @Value("${config.serviceurl.lsrp.rateshop}")
    private String carrierRateShopUrl;
    @Value("${config.serviceurl.lsrp.transit-time}")
    private String transitTimeUrl;
    private static final String CARRIER_CODE = LASERSHIP;

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
        log.debug("LaserShipRulesProcessor.loadRates()");
        List<RateshopRatesDTO> rateshopRates = List.of();
        try {
            if (shipOptions.getRulesInclude().isEmpty()) {
                return rateshopRates;
            }

            LocationItem addressFrom = shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom();
            String originEntityCode = String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode());
            LocationItem addressTo = shipmentInfo.getShipmentHeader().getDestination().getAddressTo();
            OrderInfo orderDetails = shipmentInfo.getOrderDetails();
            DimensionItem dimensionDetails = shipmentInfo.getShipmentPieces().get(0).getDimensionDetails();
            double weightValue = shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue();

            String key = String.format(RATE_KEY, CARRIER_CODE,
                    addressFrom.getZipCode(),
                    addressTo.getZipCode(),
                    getRateShopCacheWeight(weightValue));

            List<String> configuredMode = shipOptions.getRulesInclude().stream()
                    .filter(rulesIncluded -> rulesIncluded.getTargetCarrierCode().equals(CARRIER_CODE))
                    .map(rulesMap -> String.valueOf(rulesMap.getTargetCarrierModeCode())).toList();
            TransitInfo transitDetails = shipmentInfo.getTransitDetails();
            Long plannedShipDate = shipOptions.getRulesInclude().stream()
                    .filter(rulesIncluded -> rulesIncluded.getTargetCarrierCode().equals(CARRIER_CODE))
                    .map(RulesInclude::getPlannedShipDate)
                    .findFirst().get();
            String utcExpectedDepartureStr = epochToIso8601String(plannedShipDate); // convert planned ship date to ISO8601 format
            RateShopCarrierInput rateShopInput = buildRateShopCarrierInput(orderDetails, transitDetails, originEntityCode, addressFrom, addressTo, dimensionDetails, weightValue, utcExpectedDepartureStr, configuredMode, plannedShipDate);

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

    private RateShopCarrierInput buildRateShopCarrierInput(OrderInfo orderDetails, TransitInfo transitDetails, String originEntityCode, LocationItem addressFrom, LocationItem addressTo, DimensionItem dimensionDetails,
                                                                  double weightValue, String utcExpectedDepartureStr, List<String> configuredMode, Long plannedShipDate) {
        return RateShopCarrierInput.builder()
                .reference1(String.valueOf(orderDetails.getOrderId()))
                .reference2(String.valueOf(orderDetails.getTCLPNID()))
                .originEntityCode(originEntityCode)
                .originCountry(String.valueOf(addressFrom.getCountry()))
                .originZipCode(String.valueOf(addressFrom.getZipCode()))
                .destinationAddress(String.valueOf(addressTo.getDescription1()))
                .destinationAddress2(String.valueOf(addressTo.getDescription2()))
                .destinationCity(String.valueOf(addressTo.getCity()))
                .destinationState(String.valueOf(addressTo.getState()))
                .destinationCountry(String.valueOf(addressTo.getCountry()))
                .destinationZipCode(String.valueOf(addressTo.getZipCode()))
                .weight(weightValue)
                .width(dimensionDetails.getWidth())
                .height(dimensionDetails.getHeight())
                .length(dimensionDetails.getLength())
                .valueOfContent(orderDetails.getDeclaredValueDetails().getValue())
                .isResidential(transitDetails.getIsResidential())
                .isMilitary(orderDetails.getIsMilitary())
                .utcExpectedDeparture(utcExpectedDepartureStr)
                .plannedShipDate(plannedShipDate)
                .modeCodes(Set.copyOf(configuredMode)).build();
    }

}