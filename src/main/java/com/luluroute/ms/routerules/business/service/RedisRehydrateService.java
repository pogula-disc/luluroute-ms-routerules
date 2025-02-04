package com.luluroute.ms.routerules.business.service;

import com.logistics.luluroute.avro.artifact.message.RouteRules;
import com.logistics.luluroute.domain.rateshop.carrier.*;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.redis.shipment.tranisttime.TransitTimeCache;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import com.luluroute.ms.routerules.business.entity.TransitTimeRateCache;
import com.luluroute.ms.routerules.business.exceptions.ShipmentMessageException;
import com.luluroute.ms.routerules.business.mapper.RulesMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_FIELD_INFO;
import static com.luluroute.ms.routerules.business.util.Constants.VEHO;

@Service
@Slf4j
public class RedisRehydrateService {

    @Value("${config.serviceurl.entity}")
    private String entityUrl;

    @Value("${config.serviceurl.carrier}")
    private String carrierUrl;

    @Autowired
    private RestPublisher<EntityPayload, Object> entityPayloadRestPublisher;

    @Autowired
    private RestPublisher<CarrierMainPayload, Object> carrierMainPayloadRestPublisher;

    @Autowired
    private RedisCacheLoader redisCacheLoader;
    @Autowired
    private RestPublisher<RateShopCarrierInput, RateShopCarrierResponse> rateShopPublisher;

    /**
     * Gets the entity by code.
     *
     * @param key the entity code
     * @return the entity by code
     */
    public EntityPayload getEntityByCode(String key) {
        EntityPayload entityPayload = redisCacheLoader.getEntityByCode(key);
        log.info(String.format(STANDARD_FIELD_INFO, "Loading Store Profile from Redis", key));
        if (ObjectUtils.isEmpty(entityPayload)) {
            log.info(String.format(STANDARD_FIELD_INFO, "Loading Store Profile from DB", key));
            entityPayload = entityPayloadRestPublisher.performRestAPICall(entityUrl, key, EntityPayload.class);
        }
        return entityPayload;
    }

    public List<RateshopRatesDTO> getRateForCarrier(
            String key, RateShopCarrierInput rateShopInput, String apiURL, String carrierCode, RouteRules shipOptions)
            throws ShipmentMessageException {
        RateShopCarrierRatesCache rateShopCarrierRatesCache = redisCacheLoader.getRateForCarrier(key);
        List<RateshopRatesDTO> rateshopRates;
        if (ObjectUtils.isEmpty(rateShopCarrierRatesCache)
                || routeExistsWithoutRate(rateShopCarrierRatesCache, shipOptions)) {
            log.info(String.format(STANDARD_FIELD_INFO, "Loading Rate from API", key));
            RateShopCarrierResponse rateShopCarrierResponse =
                    rateShopPublisher.performPostRestCall(apiURL, rateShopInput, new ParameterizedTypeReference<>(){});
            if(ObjectUtils.isNotEmpty(rateShopCarrierResponse.getError())) {
                RateShopCarrierError error = rateShopCarrierResponse.getError();
                throw new ShipmentMessageException(Integer.toString(error.getErrorCode()), error.getDescription(), error.getSource());
            }
            rateshopRates = RulesMapper.INSTANCE.mapResponseModesToDTOs(rateShopCarrierResponse.getModes());
        } else {
            rateshopRates = RulesMapper.INSTANCE.mapCacheModesToDTOs(rateShopCarrierRatesCache.getModes());
        }
        rateshopRates.forEach(rate -> rate.setCarrierCode(carrierCode));
        return rateshopRates;
    }

    public CarrierMainPayload getCarrierByCode(String key) {
        CarrierMainPayload carrierMainPayload = redisCacheLoader.getCarrierByCode(key);
        log.debug(String.format(STANDARD_FIELD_INFO, "Loading Carrier Profile from Redis", key));
        if (ObjectUtils.isEmpty(carrierMainPayload)) {
            log.debug(String.format(STANDARD_FIELD_INFO, "Loading Carrier Profile from DB", key));
            carrierMainPayload = carrierMainPayloadRestPublisher.performRestAPICall(carrierUrl, key, CarrierMainPayload.class);
        }
        return carrierMainPayload;
    }

    private static boolean routeExistsWithoutRate(
            RateShopCarrierRatesCache rateShopResponse, RouteRules shipOptions) {
        Set<String> availableRateModes = rateShopResponse.getModes().stream()
                .map(RateShopCarrierRatesCacheMode::getModeCode)
                .collect(Collectors.toUnmodifiableSet());
        return shipOptions.getRulesInclude().stream()
                .anyMatch(route -> !availableRateModes.contains(route.getTargetCarrierModeCode().toString()));
    }

    public List<RateshopRatesDTO> getRateForCarrier(String key, String carrierCode) {
        RateShopCarrierRatesCache rateShopCarrierRatesCache = redisCacheLoader.getRateForCarrier(key);
        List<RateshopRatesDTO> rateshopRatesDTOS = new ArrayList<>();
        rateShopCarrierRatesCache.getModes().stream().forEach(rateShopCarrierRatesCacheMode -> {
            RateshopRatesDTO rateshopRatesDTO =
                    RateshopRatesDTO.builder().modeCode(rateShopCarrierRatesCacheMode.getModeCode())
                            .baseRate(rateShopCarrierRatesCacheMode.getBaseRate())
                            .carrierCode(carrierCode).build();
            rateshopRatesDTOS.add(rateshopRatesDTO);
        });
        return rateshopRatesDTOS;
    }
    
    public TransitTimeRateCache hydrateTransitTimeRateCache(String cacheKey) {
    	return redisCacheLoader.getVehoTransitTimeRateCacheByKey(cacheKey);
    }
    
    public TransitTimeRateCache cacheNewTransitTimeRate(String carrierCode,String originEntityCode,  String destPostalCode, int transitDays,Double rate) {
    	TransitTimeRateCache newTransitTimeCache = TransitTimeRateCache.builder()
                .originEntityCode(originEntityCode)
                .destPostalCode(destPostalCode)
                .transitDays(transitDays)
                .rate(rate)
                .build();
        String transitTimeRateCacheKey = buildTransitTimeRateCacheKey(carrierCode,originEntityCode, destPostalCode);
        redisCacheLoader.saveVehoTransitTimeRateCacheByKey(transitTimeRateCacheKey,newTransitTimeCache);
        return newTransitTimeCache;
    }
    public String buildTransitTimeRateCacheKey(String carrierCode,String originEntityCode, String destPostalCode) {
    	return String.format("%s-%s-%s", carrierCode,originEntityCode, destPostalCode);
    }
}
