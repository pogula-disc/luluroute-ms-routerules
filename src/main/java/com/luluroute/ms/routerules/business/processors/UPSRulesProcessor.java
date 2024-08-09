package com.luluroute.ms.routerules.business.processors;

import static com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierHelper.getRateShopCacheWeight;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_DATA_RATE_SOURCE;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_RATE;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.eliminateAllShipOptionsForError;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_KEY;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_NOT_FOUND;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.util.Constants.UPS;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

@Slf4j
@Component
public class UPSRulesProcessor extends AbstractRulesProcessor {

	@Value("${config.serviceurl.ups.transit-time}")
	private String transitTimeUrl;
	@Value("${config.serviceurl.ups.rateshop}")
	private String carrierRateShopUrl;
	private static final String CARRIER_CODE = UPS;

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
		log.debug("UPSRulesProcessor.loadRates()");
		List<RateshopRatesDTO> rateshopRates = List.of();
		try {
			if (shipOptions.getRulesInclude().isEmpty()) {
				return rateshopRates;
			}

			String key = String.format(RATE_KEY, CARRIER_CODE,
					shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode(),
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode(),
					getRateShopCacheWeight(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue()));

			List<String> configuredMode = shipOptions.getRulesInclude().stream()
					.filter(rulesIncluded -> rulesIncluded.getTargetCarrierCode().equals(CARRIER_CODE))
					.map(rulesMap -> String.valueOf(rulesMap.getTargetCarrierModeCode())).toList();

			boolean isDimensionsEmpty = isAllDimensionEmpty(
					shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getWidth(),
					shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getLength(),
					shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getHeight());

			log.debug("Rules / modes configured ", configuredMode);

			RateShopCarrierInput rateShopInput = RateShopCarrierInput.builder()
					.originCountry(
							String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry()))
					.originZipCode(
							String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode()))
					.destinationZipCode(String
							.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode()))
					.destinationCountry(String
							.valueOf(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry()))
					.weight(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue())
					.width(isDimensionsEmpty ? 1
							: shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getWidth())
					.height(isDimensionsEmpty ? 1
							: shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getHeight())
					.length(isDimensionsEmpty ? 1
							: shipmentInfo.getShipmentPieces().get(0).getDimensionDetails().getLength())
					.isMilitary(shipmentInfo.getOrderDetails().getIsMilitary())
					.valueOfContent(null != shipmentInfo.getOrderDetails().getDeclaredValueDetails()
							? shipmentInfo.getOrderDetails().getDeclaredValueDetails().getValue()
							: 0)
					.currency(null != shipmentInfo.getOrderDetails().getDeclaredValueDetails()
							? String.valueOf(shipmentInfo.getOrderDetails().getDeclaredValueDetails().getCurrency())
							: "USD")
					.originEntityCode(String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode()))
					.originStateProvinceCode(
							String.valueOf(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getState()))
					.orderType(String.valueOf(shipmentInfo.getOrderDetails().getOrderType()))
					.modeCodes(Set.copyOf(configuredMode)).build();

			log.info(String.format(STANDARD_FIELD_INFO, "RateShopRateKey #", key));
			rateshopRates = redisCacheLoader
					.getRateForCarrier(key, rateShopInput, carrierRateShopUrl, CARRIER_CODE, shipOptions).stream()
					.filter(rate -> updateRateForShipOption(CARRIER_CODE, rate, shipOptions)).toList();
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

	private boolean isAllDimensionEmpty(Double width, Double length, Double height) {
		return (width == null || width == 0) && (length == null || length == 0) && (height == null || height == 0);
	}
}
