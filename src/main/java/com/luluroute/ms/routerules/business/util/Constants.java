package com.luluroute.ms.routerules.business.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    public static final String MESSAGE_REDIS_KEY_LOADING = "APP_MESSAGE=\"Loading %s for key %s from Redis";
    public static final String STANDARD_EXCEPTION_MESSAGE = "APP_MESSAGE=Description # \"{%s}\" | Stacktrace # \"{%s}\" ";
    public static final String STANDARD_ERROR = "APP_MESSAGE=Error Occurred | METHOD=\"{}\" | ERROR=\"{}\"";
    public static final String STANDARD_DATA_ERROR = "APP_MESSAGE=Data Error Occurred | METHOD=\"{%s}\" | FIELD=\"{%s}\" | ERROR=\"{%s}\"";
    public static final String X_PRIMARY_SOURCE_ENTITY = "X-Primary-Source-Entity";
    public static final String X_SHIPMENT_CORRELATION_ID = "X-Shipment-Correlation-Id";
    public static final String X_MESSAGE_CORRELATION_ID = "X-Message-Correlation-Id";
    public static final String X_JOB_START_TIME = "X-Job-Start-Time";
    public static final String PROCESSING_SHIPMENT_REQUEST = "APP_MESSAGE=\"Processing starting for ShipmentCorrelationId {%s} Message\" | message=\"{%s}\"";
    public static final String PROFILE_MESSAGE = "APP_MESSAGE=\"Profile %s \" | key=\"{%s}\"  | value=\"{%s}\" | ShipmentCorrelationId=\"{%s}\"";
    public static final String ROUTE_RULES_CONSUMER = "Consumer_ROUTE_RULES";
    public static final String MESSAGE = "APP_MESSAGE=\"Received Avro %s Message\" | message=\"{%s}\"";
    public static final String SHIPMENT_REQUEST_RELEASE = "2000";
    public static final String STANDARD_WARN = "APP_MESSAGE=Warn Occurred | METHOD=\"{}\" | WARN=\"{}\"";
    public static final String STANDARD_FIELD_INFO = "APP_MESSAGE=Field Identifier# \"{%s}\" | Value # \"{%s}\" ";
    public static final String STANDARD_FIELD_INFO1 = "APP_MESSAGE=Field Identifier# \"{%s}\" | Rule Code # \"{%s}\" | CarrierCode # \"{%s}\"";
    public static final String PARSER_ERROR_FORMAT = "Parsing Datetime Exception, Field:{%s} , Exception=\"{%s}\"";
    public static final Date NULL = null;
    public static final String REDIS_RATE_SHOP_KEY = "MSCR-%s-RS-%s-%s";
    public static final String REDIS_TRANSIT_TIME_KEY = "MSCR-%s-TT-%s-%s";
    public static final String MESSAGE_PUBLISHED = "APP_MESSAGE=Message Published | Key=\"{}\" | Message=\"{}\" | Topic=\"{}\"";
    public static final String BUILD_ARTIFACT_ROUTE_RULE = "5100";
    public static final long BUILD_ARTIFACT_STATUS_COMPLETE = 200;
    public static final long BUILD_ARTIFACT_STATUS_ERROR = 500;
    public static final long ROUTE_RULE_PROCESS_CODE = 600;
    public static final long PROCESS_STATUS_STARTED = 100;
    public static final long PROCESS_STATUS_ENDED = 190;
    public static final long PROCESS_STATUS_COMPLETED = 200;
    public static final long PROCESS_STATUS_FAILED = 500;
    public static final String ROUTE_RULE_PROCESS_NAME = "Route Rules Process";
    public static final String RATE_NOT_FOUND = "Business Rules: No rate found. Carrier # (%s) Service Mode # (%s) Exceptions # (%s)";
    public static final String ROUTE_RULES_NOT_FOUND = "Business Rules: No service found in rules mapping";
    public static final String ROUTE_RULES_RATE_NOT_FOUND = "Business Rules: Not able to identify rate for ship options";
    public static final String ROUTE_RULES_NOT_PROCESSES = "Business Rules: Error Occurred | Processing the carrier rule";
    public static final String ROUTE_RULES_TRANSIT_TIME_ERROR = "Transit Time API: Requested Delivery Date can't be validated.";
    public static final String ROUTE_RULES_RESPONSE_DELIVERY_DATE_ERROR = "Business Rules: Error Occurred | Processing the response delivery date";
    public static final String ROUTE_RULES_ENTITY_NOT_FOUND = "Business Rules: Pre-requisite Failed | Entity {%s} not available ";
    public static final String ROUTE_RULES_TRANSIT_TIME_RATE_NOT_FOUND = "Business Rules: Pre-requisite Failed | TRANSIT_TIME_RATE {%s} not available ";
    public static final String ROUTE_RULES_NOT_RECOMMENDED_SERVICE_FOUND = "No recommended service Found.";
    public static final String ROUTE_RULES_NOT_FOUND_CALC = "%sBusiness Rules: Planned Delivery Date (%s) can't be met. Carrier Delivery Date (%s). Carrier error (%s).";
    public static final String STRING_EMPTY = "";
    public static final String ELLIPSIS = "...";
    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_TRANSACTION_REFERENCE = "X-Transaction-Reference";
    public static final String CODE_PATH = "codePath";
    public static final String STANDARD_INFO = "APP_MESSAGE= METHOD=\"{%s}\" | Identifier# \"{%s}\" | Value # \"{%s}\" ";
    public static final String DESCARTES = "DCRT";
    public static final String FEDEX = "FEDX";
    public static final String GENERIC = "GENC";
    public static final String AUPOST_STARTRACK = "STRK";
    public static final String CARRIER_CODE = "CarrierCode";
    public static final String USPS = "USPS";
    public static final String GOBOLT = "GBLT";
    public static final String TFORCE_RETAIL = "TFIS";
    public static final String TFORCE_ECOMM = "TFEC";
    public static final String UPS = "UPSC";
    public static final String LASERSHIP = "lasership";
    public static final String VEHO = "VEHO";
    public static final String CNP = "CNP";
    public static final String RATE_KEY = "%s-%s-%s-%s";
    public static final String CARRIER_MODE = "%s-%s";
    public static final String PROCESS_EXCEPTION_DETAIL = "%s-%s %s | ";
    public static final String IS_MOCK_ENABLED = "Is-Mock-Enabled";
    public static final String EMPTY = "";
    public static final String REQUESTED_CARRIER_CODE_AND_MODE = "%s_%s";

}
