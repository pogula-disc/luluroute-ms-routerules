package com.luluroute.ms.routerules.business.repository;

import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.luluroute.ms.routerules.business.util.QueryConstants.*;

@Repository
public interface BusinessRouteRulesRepository extends JpaRepository<BusinessRouteRulesEntity, UUID> {

    @Query(value = RULE_FILTER_QUERY, nativeQuery = true)
    List<BusinessRouteRulesEntity> loadApplicablesRules(@Param("targetCarrierCodes") Set<String> targetCarrierCodes,
                                                        @Param("hazmat") int hazmat,
                                                        @Param("orderType") String orderType,
                                                        @Param("srcPrimaryEntity") String  srcPrimaryEntity,
                                                        @Param("shipVia") String shipVia,
                                                        @Param("laneName") String laneName,
                                                        @Param("isPOBox") String isPOBox,
                                                        @Param("isMilitary") String isMilitary,
                                                        @Param("isResidential") String isResidential,
                                                        @Param("srcCountry") String sourceCountry,
                                                        @Param("srcState") String sourceState,
                                                        @Param("srcCity") String sourceCity,
                                                        @Param("srcZip") String sourceZip,
                                                        @Param("dstCountry") String destinationCountry,
                                                        @Param("dstState") String destinationState,
                                                        @Param("dstCity") String destinationCity,
                                                        @Param("dstZip") String destinationZip,
                                                        @Param("weight") double weight
    );

    Optional<BusinessRouteRulesEntity> findByRouteRuleId(UUID routeRuleId);
    Optional<BusinessRouteRulesEntity> findByActiveAndRouteRuleId(int active, UUID routeRuleId);
    @Query(value = SELECT_ALL_QUERY + RULE_SEARCH_QUERY,
            countQuery = SELECT_COUNT_QUERY + RULE_SEARCH_QUERY,
            nativeQuery = true)
    Page<BusinessRouteRulesEntity> searchRouteRulesNative(String name, String ruleType, String targetCarrierCode, String targetCarrierModeCode, int[] enabledList, int[] hazmatList, String orderType, Pageable pageRequest);

}
