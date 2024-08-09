package com.luluroute.ms.routerules.business.mapper;

import com.logistics.luluroute.avro.artifact.message.RulesError;
import com.logistics.luluroute.avro.artifact.message.RulesInclude;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCacheMode;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierResponseMode;
import com.luluroute.ms.routerules.business.dto.RateshopRatesDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;


@Service
@Mapper(uses = {CommonMapper.class})
public interface RulesMapper {


    RulesMapper INSTANCE = Mappers.getMapper(RulesMapper.class);

    @Mapping(source="shipOption", target=".")
    @Mapping(source="errorCode", target="processException.code")
    @Mapping(source="errorMessage", target="processException.description")
    @Mapping(source="errorSource", target="processException.source")
    RulesError mapShipOptionToError(RulesInclude shipOption, String errorCode, String errorMessage, String errorSource);

    List<RateshopRatesDTO> mapCacheModesToDTOs(Set<RateShopCarrierRatesCacheMode> rateShopCarrierRatesCache);

    List<RateshopRatesDTO> mapResponseModesToDTOs(Set<RateShopCarrierResponseMode> rateShopCarrierResponseModes);
}
