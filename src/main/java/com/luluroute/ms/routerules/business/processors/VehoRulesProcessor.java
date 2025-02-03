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
import com.logistics.luluroute.redis.shipment.tranisttime.TransitTimeCache;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.dto.TransitTimeResponse;
import com.luluroute.ms.routerules.business.entity.TransitTimeRateCache;
import com.luluroute.ms.routerules.business.exceptions.DefaultTransitTimeFailureException;
import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.processors.helper.VehoRuleHelper;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierHelper.getRateShopCacheWeight;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_NO_DATA_RATE_SOURCE;
import static com.luluroute.ms.routerules.business.exceptions.ExceptionConstants.CODE_PROFILE_RATE;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.addShipOptionToErrors;
import static com.luluroute.ms.routerules.business.processors.helper.BusinessRuleHelper.eliminateAllShipOptionsForError;
import static com.luluroute.ms.routerules.business.util.Constants.LASERSHIP;
import static com.luluroute.ms.routerules.business.util.Constants.VEHO;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_KEY;
import static com.luluroute.ms.routerules.business.util.Constants.RATE_NOT_FOUND;
import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.util.Constants.ROUTE_RULES_TRANSIT_TIME_RATE_NOT_FOUND;
import static com.luluroute.ms.routerules.business.util.ExceptionConstants.CODE_PROFILE;
import static com.luluroute.ms.routerules.business.util.ExceptionConstants.CODE_PROFILE_SOURCE;
import static com.luluroute.ms.routerules.business.util.DateUtil.epochToIso8601String;

@Slf4j
@Component
public class VehoRulesProcessor extends AbstractRulesProcessor {

    @Value("${config.serviceurl.veho.transit-time-rate}")
    private String transitTimeUrl;
    private static final String CARRIER_CODE = VEHO;
    
    @Autowired
    private VehoRuleHelper vehoRuleHelper;

    @Override
    public void updateShipOptionDates(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile)
            throws MappingFormatException, ShipmentMessageException {
        super.updatePlannedShipDateForShipOptions(shipOptions, shipmentInfo, entityProfile);
    	
        String originEntityCode = shipmentInfo.getShipmentHeader().getOrigin().getEntityCode().toString();
        String destPostalCode = shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode().toString();

        String transitTimeRateCacheKey = redisCacheLoader.buildTransitTimeRateCacheKey(CARRIER_CODE,originEntityCode,destPostalCode);
        TransitTimeRateCache transitTimeRateCache= redisCacheLoader.hydrateTransitTimeRateCache(transitTimeRateCacheKey);
        
        List<TransitTimeResponse> transitTimeResponse = prepareTransitTimeRateCache(shipOptions, shipmentInfo, entityProfile,
        		originEntityCode, destPostalCode,transitTimeRateCache);
        
    	vehoRuleHelper.updatePlannedDeliveryDate(shipOptions,shipmentInfo,entityProfile,transitTimeResponse);
    }

	@Override
	public List<RateshopRatesDTO> loadRates(RouteRules shipOptions, ShipmentInfo shipmentInfo) {
		log.debug("VehoRulesProcessor.loadRates()");
		List<RateshopRatesDTO> rateshopRates = new ArrayList<>();
		try {
			if (shipOptions.getRulesInclude().isEmpty()) {
				return rateshopRates;
			}

			String originEntityCode = shipmentInfo.getShipmentHeader().getOrigin().getEntityCode().toString();
	        String destPostalCode = shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode().toString();
	        
	        String transitTimeRateCacheKey = redisCacheLoader.buildTransitTimeRateCacheKey(CARRIER_CODE,originEntityCode,destPostalCode);
	        TransitTimeRateCache transitTimeRateCache= redisCacheLoader.hydrateTransitTimeRateCache(transitTimeRateCacheKey);
	        
	        EntityPayload entityProfile =   vehoRuleHelper.loadEntityProfile(originEntityCode);
	        List<TransitTimeResponse> transitTimeResponse = prepareTransitTimeRateCache(shipOptions, shipmentInfo, entityProfile,
	        		originEntityCode, destPostalCode,transitTimeRateCache);
			
			log.info(String.format(STANDARD_FIELD_INFO, "RateShopRateKey #", transitTimeRateCacheKey));
			
			if(transitTimeResponse.size()>0) {
		        shipOptions.getRulesInclude().forEach(shipOption -> {
		        	RateshopRatesDTO rateDTO = RateshopRatesDTO.builder().baseRate(transitTimeResponse.get(0).getRate())
		                    .carrierCode(shipOption.getTargetCarrierCode().toString())
		                    .modeCode(shipOption.getTargetCarrierModeCode().toString())
		                    .build();
		        	updateRateForShipOption(shipOption.getTargetCarrierCode().toString(), rateDTO, shipOptions);
		        	rateshopRates.add(rateDTO);
		        });
		        log.debug(String.format(STANDARD_FIELD_INFO, "Rates # ", rateshopRates));
			}
	        
	        eliminateShipOptionsWithoutRates(shipOptions);

		} catch (Exception e) {
			eliminateAllShipOptionsForError(shipOptions, new ShipmentMessageException(CODE_PROFILE_RATE,
					String.format(RATE_NOT_FOUND, CARRIER_CODE, "all", e.getMessage()), CODE_NO_DATA_RATE_SOURCE));
		}
		return rateshopRates;
	}
    
    public List<TransitTimeResponse> buildTransitTimeResponse(RouteRules shipOptions,int transitDays,Double rate) {
    	
    	List<TransitTimeResponse> responseList = new ArrayList<>();
    	shipOptions.getRulesInclude().forEach(shipOption -> {
    			TransitTimeResponse transitTimeResp = TransitTimeResponse.builder()
    	    			.responseDeliveryDate(Instant.now().toEpochMilli())
    	    			.carrierMode(shipOption.getTargetCarrierModeCode().toString())
    	    			.failureMessage(null)
    	    			.transitDays(transitDays)
    	    			.rate(rate)
    	    			.build();
    			responseList.add(transitTimeResp);
    			
    	});
    	
    	return responseList;
    }
    
    public List<TransitTimeResponse> prepareTransitTimeRateCache(RouteRules shipOptions, ShipmentInfo shipmentInfo, EntityPayload entityProfile,
    		String originEntityCode, String destPostalCode,TransitTimeRateCache transitTimeRateCache) throws MappingFormatException,ShipmentMessageException{
    	
    	List<TransitTimeResponse> transitTimeResponse = new ArrayList<>();
    	if(!ObjectUtils.isEmpty(transitTimeRateCache)) {
    		log.info("Transit Time {} and Rate {} present in TransitTimeRateCache  ",transitTimeRateCache.getTransitDays(),transitTimeRateCache.getRate());
    		transitTimeResponse = buildTransitTimeResponse(shipOptions,transitTimeRateCache.getTransitDays(),transitTimeRateCache.getRate());
    	}
    	else {
    		List<TransitTimeRequest> validTransitTimeRequests = super.getTransitTimeRequests(shipOptions, shipmentInfo, entityProfile);
    		transitTimeResponse = super.callCarrierTransitTime(validTransitTimeRequests, transitTimeUrl);
    		
    		transitTimeResponse.forEach(transitTimeRate -> {
    			if(transitTimeRate != null && StringUtils.isEmpty(transitTimeRate.getFailureMessage())) {
    				 redisCacheLoader.cacheNewTransitTimeRate(CARRIER_CODE,originEntityCode, destPostalCode, 
    					transitTimeRate.getTransitDays(),transitTimeRate.getRate());
    			}
    		});
    	}
    	return transitTimeResponse;
    }
}