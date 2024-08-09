package com.luluroute.ms.routerules.business.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonBinaryType.class)
@TypeDef(name = "orderType", typeClass = OrderType.class)
@Table(name = "rtrlrouterule", schema = "domain")

public class BusinessRouteRulesEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "routeruleid", unique = true, nullable = false)
	private UUID routeRuleId;

	@Column(name = "rulecode", nullable = false)
	private String ruleCode;

	@Column(name = "responsenotes")
	private String responseNotes;

	@Column(name = "ruletype", nullable = false)
	private String ruleType;

	@Column(name = "applytype", nullable = false)
	private String applyType;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description", nullable = false)
	private String description;

	@Column(name = "enabled", nullable = false)
	private int enabled;

	@Column(name = "priority", nullable = false)
	private int priority;

	@Column(name = "targetcarriercode")
	private String targetCarrierCode;

	@Column(name = "targetcarriermodecode")
	private String targetCarrierModeCode;

	@Column(name = "hazmat")
	private int hazmat;

	@Type(type = "json")
	@Column(name = "applicabledateranges", columnDefinition = "json")
	private List<ApplicableDateRanges> applicableDateRanges;

	@Type(type = "json")
	@Column(name = "applicableweekdaytime", columnDefinition = "json")
	private List<ApplicableWeekDayTime> applicableWeekDayTime;

	@Type(type = "json")
	@Column(name = "requesttype", columnDefinition = "json")
	private List<RequestType> requesttype;

	@Type(type = "json")
	@Column(name = "srcaccount", columnDefinition = "json")
	private List<SrcAccount> srcAccount;

	@Type(type = "json")
	@Column(name = "srcloyaltytype", columnDefinition = "json")
	private List<SrcLoyaltyType> srcLoyaltyType;

	@Type(type = "json")
	@Column(name = "srcrequestedcarrier", columnDefinition = "json")
	private List<SrcRequestedCarrier> srcRequestedCarrier;

	@Type(type = "json")
	@Column(name = "srcweightdimension", columnDefinition = "json")
	private List<SrcWeightDimension> srcWeightDimension;

	@Type(type = "json")
	@Column(name = "srcdeclaredvalue", columnDefinition = "json")
	private List<SrcDeclaredValue> srcDeclaredValue;

	@Type(type = "json")
	@Column(name = "srcchannel", columnDefinition = "json")
	private List<SrcChannel> srcChannel;

	@Type(type = "json")
	@Column(name = "srcpath", columnDefinition = "json")
	private List<SrcPath> srcPath;

	@Type(type = "json")
	@Column(name = "srcaddresses", columnDefinition = "json")
	private List<SrcAddresses> srcAddresses;

	@Type(type = "json")
	@Column(name = "dstaddresses", columnDefinition = "json")
	private List<DstAddresses> dstAddresses;

	@Type(type = "json")
	@Column(name = "srclocation", columnDefinition = "json")
	private List<SrcLocation> srcLocation;

	@Type(type = "json")
	@Column(name = "dstlocation", columnDefinition = "json")
	private List<DstLocation> dstLocation;

	@Type(type = "json")
	@Column(name = "srcprimaryentitycode", columnDefinition = "json")
	private List<SrcPrimaryEntityCode> srcPrimaryEntityCode;

	@Type(type = "json")
	@Column(name = "dstprimaryentitycode", columnDefinition = "json")
	private List<DstPrimaryEntityCode> dstPrimaryEntityCode;

	@Type(type = "json")
	@Column(name = "srcsecondaryentitycode", columnDefinition = "json")
	private List<SrcSecondaryEntityCode> srcSecondaryEntityCode;

	@Type(type = "json")
	@Column(name = "dstczipcode", columnDefinition = "json")
	private List<DstZipCode> dstZipCode;

	@Type(type = "json")
	@Column(name = "serviceattributes", columnDefinition = "json")
	private List<ServiceAttributes> serviceAttributes;

	@Type(type = "json")
	@Column(name = "shipvia", columnDefinition = "json")
	private List<ShipVia> shipvia;

	@Type(type = "json")
	@Column(name = "laneName", columnDefinition = "json")
	private List<LaneName> laneName;

	@Type(type = "json")
	@Column(name = "ordertype", columnDefinition = "json")
	private List<OrderType> orderType;

	@Type(type = "json")
	@Column(name = "productcode", columnDefinition = "json")
	private List<ProductCode> productcode;

	@Column(name = "ref_1")
	private String ref1;

	@Column(name = "ref_2")
	private String ref2;

	@Column(name = "ref_3")
	private String ref3;

	@Column(name = "active", nullable = false)
	private int active;

	@Column(name = "createddate", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdDate;

	@Column(name = "createdby", nullable = false)
	private String createdBy;

	@Column(name = "updateddate")
	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedDate;

	@Column(name = "updatedby")
	private String updatedBy;

}