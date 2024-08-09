package com.luluroute.ms.routerules.business.util;

public class QueryConstants {

    private QueryConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SELECT_ALL_QUERY = "SELECT r.* ";

    public static final String SELECT_COUNT_QUERY = "SELECT count(r.*) ";

    public static final String RULE_SEARCH_QUERY = " FROM domain.rtrlrouterule as r " +
            "WHERE (:name IS NULL OR :name = '' OR r.name = :name) AND " +
            "(:ruleType IS NULL OR :ruleType = '' OR r.ruletype = :ruleType) AND (:targetCarrierCode IS NULL OR :targetCarrierCode = '' OR r.targetcarriercode = :targetCarrierCode) AND " +
            "(:targetCarrierModeCode is null OR :targetCarrierModeCode = '' OR r.targetcarriermodecode = :targetCarrierModeCode) AND " +
            "(r.enabled IN (:enabledList)) AND (r.hazmat IN (:hazmatList)) AND " +
            "(domain.evaluate_json_attribute(R.ordertype, 'ordertype', :orderType) = 1) ";


    public static final String RULE_FILTER_QUERY = " SELECT  * FROM DOMAIN.evaluate_route_rules" +
            "(domain.list_to_array(:targetCarrierCodes), :hazmat , :orderType, :srcPrimaryEntity, :shipVia, :laneName, :isPOBox, :isMilitary," +
            " :isResidential, :srcCountry, :srcState, :srcCity, :srcZip, :dstCountry, :dstState, :dstCity, :dstZip, :weight" +
            ", 1)";
}
