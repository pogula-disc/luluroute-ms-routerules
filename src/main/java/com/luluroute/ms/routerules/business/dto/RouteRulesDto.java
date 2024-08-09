package com.luluroute.ms.routerules.business.dto;

import com.luluroute.ms.routerules.business.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteRulesDto {
	private UUID routeRuleId;
	@NotNull
	private String ruleCode;
	@NotNull
	private String ruleType;
	@NotNull
	private String applyType;
	@NotNull
	private String name;
	@NotNull
	private String description;
	private String responseNotes;
	@NotNull
	private int enabled;
	@NotNull
	private int priority;
	@NotNull
	private int hazmat;
	private String targetCarrierCode;
	private String targetCarrierModeCode;
	private ApplicableDateRanges[] applicableDateRanges;
	private ApplicableWeekDayTime[] applicableWeekDayTime;
	private RequestType[] requestType;
	private SrcAccount[] srcAccount;
	private SrcLoyaltyType[] srcLoyaltyType;
	private SrcRequestedCarrier[] srcRequestedCarrier;
	private SrcWeightDimension[] srcWeightDimension;
	private SrcDeclaredValue[] srcDeclaredValue;
	private SrcChannel[] srcChannel;
	private SrcPath[] srcPath;
	private SrcAddresses[] srcAddresses;
	private DstAddresses[] dstAddresses;
	private SrcLocation[] srcLocation;
	private DstLocation[] dstLocation;
	private SrcPrimaryEntityCode[] srcPrimaryEntityCode;
	private DstPrimaryEntityCode[] dstPrimaryEntityCode;
	private SrcSecondaryEntityCode[] srcSecondaryEntityCode;
	private DstZipCode[] dstZipCode;
	private ServiceAttributes[] serviceAttributes;
	private ShipVia[] shipvia;
	private OrderType[] orderType;
	private ProductCode[] productcode;
	private String ref1;
	private String ref2;
	private String ref3;
	private int active;
	private Date createdDate;
	@NotNull
	private String createdBy;
	private Date updatedDate;
	private String updatedBy;
}