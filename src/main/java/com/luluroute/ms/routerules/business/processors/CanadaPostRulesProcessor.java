package com.luluroute.ms.routerules.business.processors;

import com.logistics.luluroute.avro.shipment.shared.DimensionItem;
import com.logistics.luluroute.avro.shipment.shared.LocationItem;
import com.logistics.luluroute.avro.shipment.shared.MeasureItem;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.avro.shipment.service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.routerules.business.exceptions.DefaultTransitTimeFailureException;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.processors.helper.CanadaPostRuleHelper;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

import static com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierHelper.getRateShopCacheWeight;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_DATA_RATE_SOURCE;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_RATE;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.eliminateAllShipOptionsForError;
import static com.luluroute.ms.routerules.business.util.Constants.*;

@Slf4j
@Component
public class CanadaPostRulesProcessor extends AbstractRulesProcessor {

    @Value("${config.serviceurl.cnp.rateshop}")
    private String carrierRateShopUrl;

	@Autowired
    private CanadaPostRuleHelper canadaPostRuleHelper;

    private static final String CARRIER_CODE = CNP;
	
    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile)
            throws MappingFormatException,DefaultTransitTimeFailureException {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);
        canadaPostRuleHelper.updatePlannedDeliveryDate(shipOptions,shipmentInfo,entityProfile);
        
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
            DimensionItem dimensionDetails = shipmentInfo.getShipmentPieces().get(0).getDimensionDetails();
            MeasureItem weightDetails = shipmentInfo.getShipmentPieces().get(0).getWeightDetails();
            String orderType = String.valueOf(shipmentInfo.getOrderDetails().getOrderType());
            String key = String.format(RATE_KEY, CARRIER_CODE,
                    addressFrom.getZipCode(),
                    addressTo.getZipCode(),
                    getRateShopCacheWeight(weightDetails.getValue()));

            List<String> configuredMode = shipOptions.getRulesInclude().stream()
                    .filter(rulesIncluded -> rulesIncluded.getTargetCarrierCode().equals(CARRIER_CODE))
                    .map(rulesMap -> String.valueOf(rulesMap.getTargetCarrierModeCode())).toList();
            RateShopCarrierInput rateShopInput = buildRateShopCarrierInput(originEntityCode, addressFrom, addressTo, dimensionDetails, weightDetails, configuredMode, orderType);

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

    private RateShopCarrierInput buildRateShopCarrierInput(String originEntityCode, LocationItem addressFrom, LocationItem addressTo, DimensionItem dimensionDetails,
                                                           MeasureItem weightDetails, List<String> configuredMode , String orderType) {
        return RateShopCarrierInput.builder()
                .originEntityCode(originEntityCode)
                .originZipCode(String.valueOf(addressFrom.getZipCode()))
                .originCountry(String.valueOf(addressFrom.getCountry()))
                .destinationZipCode(String.valueOf(addressTo.getZipCode()))
                .destinationCountry(String.valueOf(addressTo.getCountry()))
                .weight(weightDetails.getValue())
                .width(getValidDimension(dimensionDetails.getWidth()))
                .length(getValidDimension(dimensionDetails.getLength()))
                .height(getValidDimension(dimensionDetails.getHeight()))
                .modeCodes(Set.copyOf(configuredMode))
                .orderType(orderType)
                .build();
    }

    private Double getValidDimension(Double dimension) {
        return (dimension == null || dimension == 0) ? 1.0 : dimension;
    }
}