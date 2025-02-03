package com.luluroute.ms.routerules.business.service;

import com.logistics.luluroute.carrier.fedex.rateshop.entity.FedexRateshopZoneCache;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCache;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.redis.shipment.rateshop.RateShopPayload;
import com.logistics.luluroute.redis.shipment.tranisttime.TransitTimeCache;
import com.logistics.luluroute.redis.shipment.tranisttime.TransitTimePayload;
import com.luluroute.ms.routerules.business.entity.TransitTimeRateCache;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.routerules.business.util.Constants.MESSAGE_REDIS_KEY_LOADING;

@Slf4j
@Service
public class RedisCacheLoader {

    @Cacheable(cacheNames = "MSE01-PROFILE", key = "#key", unless = "#result == null")
    public EntityPayload getEntityByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Entity Profile", key));
        return null;
    }

    @Cacheable(cacheNames = "MSCM01-PROFILE", key = "#key", unless = "#result == null")
    public CarrierMainPayload getCarrierByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Carrier Profile", key));
        return null;
    }

    @Cacheable(cacheNames = "MSCR", key = "#key", unless = "#result == null")
    public RateShopPayload getRateShopByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "RateShop Profile", key));
        return null;
    }

    @Cacheable(cacheNames = "MSCR", key = "#key", unless = "#result == null")
    public TransitTimePayload getTransitTimeByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "TransitTime Profile", key));
        return null;
    }

    @Cacheable(cacheNames = "MSR01-RATESHOP-ZONE", key = "#key", unless = "#result == null")
    public FedexRateshopZoneCache getZoneForFedEx(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "FedEx Zone Profile", key));
        return null;
    }


    @Cacheable(cacheNames = "MSR01-RATESHOP-RATES", key = "#key", unless = "#result == null")
    public RateShopCarrierRatesCache getRateForCarrier(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Rate Profile", key));
        return null;
    }
    
    @Cacheable(value = "MSTR01-TRANSIT-TIME-RATE", key = "#vehoTransitTimeRateCacheKey", unless = "#result == null")
    public TransitTimeRateCache getVehoTransitTimeRateCacheByKey(String vehoTransitTimeRateCacheKey) {
    	log.info("in getVehoTransitTimeRateCacheByKey{} ",vehoTransitTimeRateCacheKey);
    	return null;
    }
    
    @Cacheable(value = "MSTR01-TRANSIT-TIME-RATE", key = "#vehoTransitTimeRateCacheKey")
    public TransitTimeRateCache saveVehoTransitTimeRateCacheByKey(
    		String vehoTransitTimeRateCacheKey, TransitTimeRateCache transitTimePayload) {
        return transitTimePayload;
    }
}


