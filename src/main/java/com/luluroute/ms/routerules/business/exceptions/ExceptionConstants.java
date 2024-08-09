package com.luluroute.ms.routerules.business.exceptions;

public class ExceptionConstants {
    public static final String CODE_NO_ARTIFACTS = "101";
    public static final String CODE_NO_ARTIFACTS_SOURCE = "ShipmentArtifacts";

    public static final String CODE_PROFILE = "102";
    public static final String CODE_PROFILE_SOURCE = "Redis Cache";

    public static final String CODE_ARTIFACT_CONSUME = "103";
    public static final String CODE_ARTIFACT_CONSUME_SOURCE = "ShipmentArtifacts";

    public static final String CODE_NO_DATA = "104";
    public static final String CODE_NO_DATA_SOURCE = "ShipmentMessage";

    public static final String CODE_CANCEL = "105";
    public static final String CODE_CANCEL_SOURCE = "ShipmentMessageCancel";

    public static final String CODE_UNKNOWN = "100";
    public static final String CODE_UNKNOWN_SOURCE = "Unknown";

    public static final String CODE_TRANSIT_TIME = "1030";
    public static final String CODE_TRANSIT_TIME_SOURCE = "Transit Time";

    public static final String CODE_PROFILE_DB = "1010";
    public static final String CODE_NO_DATA_DB_SOURCE = "DB";

    public static final String CODE_CARRIER_SELECTION = "1011";
    public static final String CODE_CARRIER_SELECTION_SOURCE = "Business Rules:  Carrier Rule Processing";

    public static final String CODE_PROFILE_DATE_CALC = "107";
    public static final String CODE_NO_DATA_DATE_CALC_SOURCE = "Business Rules:  Date Calculation";

    public static final String CODE_PSD_ERROR = "1022";
    public static final String CODE_CODE_PSD_CALCULATIONS = "Business Rules:  Planned ship date can't be calculated";

    public static final String CODE_PROFILE_RATE = "1023";
    public static final String CODE_NO_DATA_RATE_SOURCE = "RATE NOT FOUND";
}
