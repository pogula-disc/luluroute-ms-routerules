package com.luluroute.ms.routerules.business.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
@Table(name = "rtrlrouterule", schema = "domain")

public class RouteRulesEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "routeruleid ", unique = true, nullable = false)
    private UUID routeRuleId;

    @Column(name = " rulecode")
    private String ruleCode;

    @Column(name = " responsenotes")
    private String responseNotes;

    @Column(name = " ruletype")
    private String ruleType;

    @Column(name = " applytype")
    private String applyType;

    @Column(name = "name ")
    private String name;

    @Column(name = "description ")
    private String description;

    @Column(name = "enabled ")
    private int enabled;

    @Column(name = "priority ")
    private int priority;

    @Column(name = "targetcarriercode ")
    private String targetCarrierCode;

    @Column(name = "targetcarriermodecode ")
    private String targetCarrierModeCode;

    @Type(type = "jsonb")
    @Column(name = "applicabledateranges", columnDefinition = "jsonb")
    private ApplicableDateRanges[] applicableDateRanges;

    @Type(type = "jsonb")
    @Column(name = "applicableweekdaytime", columnDefinition = "jsonb")
    private ApplicableWeekDayTime[] applicableWeekDayTime;

    @Type(type = "jsonb")
    @Column(name = "requesttype", columnDefinition = "jsonb")
    private RequestType[] requesttype;

    @Type(type = "jsonb")
    @Column(name = "srcaccount", columnDefinition = "jsonb")
    private SrcAccount[] srcAccount;

    @Type(type = "jsonb")
    @Column(name = "srcloyaltytype", columnDefinition = "jsonb")
    private SrcLoyaltyType[] srcLoyaltyType;

    @Type(type = "jsonb")
    @Column(name = "srcrequestedcarrier", columnDefinition = "jsonb")
    private SrcRequestedCarrier[] srcRequestedCarrier;

    @Type(type = "jsonb")
    @Column(name = "srcweightdimension", columnDefinition = "jsonb")
    private SrcWeightDimension[] srcWeightDimension;

    @Type(type = "jsonb")
    @Column(name = "srcdeclaredvalue", columnDefinition = "jsonb")
    private SrcDeclaredValue[] srcDeclaredValue;

    @Type(type = "jsonb")
    @Column(name = "srcchannel", columnDefinition = "jsonb")
    private SrcChannel[] srcChannel;

    @Type(type = "jsonb")
    @Column(name = "srcpath", columnDefinition = "jsonb")
    private SrcPath[] srcPath;

    @Type(type = "jsonb")
    @Column(name = "srcaddresses", columnDefinition = "jsonb")
    private SrcAddresses[] srcAddresses;

    @Type(type = "jsonb")
    @Column(name = "dstaddresses", columnDefinition = "jsonb")
    private DstAddresses[] dstAddresses;

    @Type(type = "jsonb")
    @Column(name = "srclocation", columnDefinition = "jsonb")
    private SrcLocation[] srcLocation;

    @Type(type = "jsonb")
    @Column(name = "dstlocation", columnDefinition = "jsonb")
    private DstLocation[] dstLocation;

    @Type(type = "jsonb")
    @Column(name = "srcprimaryentitycode", columnDefinition = "jsonb")
    private SrcPrimaryEntityCode[] srcPrimaryEntityCode;

    @Type(type = "jsonb")
    @Column(name = "dstprimaryentitycode", columnDefinition = "jsonb")
    private DstPrimaryEntityCode[] dstPrimaryEntityCode;

    @Type(type = "jsonb")
    @Column(name = "srcsecondaryentitycode", columnDefinition = "jsonb")
    private SrcSecondaryEntityCode[] srcSecondaryEntityCode;

    @Type(type = "jsonb")
    @Column(name = "dstczipcode", columnDefinition = "jsonb")
    private DstZipCode[] dstZipCode;

    @Type(type = "jsonb")
    @Column(name = "serviceattributes", columnDefinition = "jsonb")
    private ServiceAttributes[] serviceAttributes;

    @Type(type = "jsonb")
    @Column(name = "shipvia", columnDefinition = "jsonb")
    private ShipVia[] shipvia;

    @Type(type = "jsonb")
    @Column(name = "ordertype", columnDefinition = "jsonb")
    private OrderType[] orderType;

    @Type(type = "jsonb")
    @Column(name = "productcode", columnDefinition = "jsonb")
    private ProductCode[] productcode;

    @Column(name = "ref_1 ")
    private String ref1;

    @Column(name = "ref_2 ")
    private String ref2;

    @Column(name = "ref_3 ")
    private String ref3;

    @Column(name = "active ")
    private int active;

    @Column(name = "createddate ")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "createdby ")
    private String createdBy;

    @Column(name = "updateddate ")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @Column(name = "updatedby ")
    private String updatedBy;

}