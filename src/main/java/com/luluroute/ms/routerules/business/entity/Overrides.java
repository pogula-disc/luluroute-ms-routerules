package com.luluroute.ms.routerules.business.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
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
import java.util.UUID;

@Entity
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = "rtrloverride", schema = "domain")
public class Overrides implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "overrideid ", unique = true, nullable = false)
    private UUID overrideId;

    @Column(name = " overridecode")
    private String overrideCode;

    @Column(name = " overridetype")
    private String overrideType;

    @Column(name = "name ")
    private String name;

    @Column(name = "description ")
    private String description;

    @Column(name = " responsenotes")
    private String responseNotes;

    @Column(name = "enabled ")
    private int enabled;

    @Column(name = "daterangeenabled ")
    private int dateRangeEnabled;

    @Column(name = "daterangeini ")
    private int dateRangeIni;

    @Column(name = "daterangeend")
    private int dateRangeEnd;

    @Type(type = "jsonb")
    @Column(name = "overridenrules", columnDefinition = "jsonb")
    private OverridenRules[] overridenRules;

    @Type(type = "jsonb")
    @Column(name = "limitcarrierusageqty", columnDefinition = "jsonb")
    private LimitCarrierUsageQty[] limitCarrierUsageQty;

    @Type(type = "jsonb")
    @Column(name = "limitcarriermodeusageqty", columnDefinition = "jsonb")
    private LimitCarrierModeUsageQty[] limitCarrierModeUsageQty;

    @Type(type = "jsonb")
    @Column(name = "limitcarrieraccountqty", columnDefinition = "jsonb")
    private LimitCarrierAccountQty[] limitCarrierAccountQty;

    @Type(type = "jsonb")
    @Column(name = "limitcarrierusageamount", columnDefinition = "jsonb")
    private LimitCarrierUsageAmount[] limitCarrierUsageAmount;


    @Type(type = "jsonb")
    @Column(name = "limitcarriermodeusageamount", columnDefinition = "jsonb")
    private LimitCarrierModeUsageAmount[] limitCarrierModeUsageAmount;

    @Type(type = "jsonb")
    @Column(name = "limitcarrieraccountamount", columnDefinition = "jsonb")
    private LimitCarrierAccountAmount[] limitCarrierAccountAmount;

    @Type(type = "jsonb")
    @Column(name = "selsourceentity", columnDefinition = "jsonb")
    private SelSourceEntity[] selSourceEntity;

    @Type(type = "jsonb")
    @Column(name = "selcarriermode", columnDefinition = "jsonb")
    private SelCarrierMode[] selCarrierMode;

    @Column(name = "applicabletype ")
    private String applicableType;

    @Column(name = "targetcarriercode ")
    private String targetCarrierCode;

    @Column(name = "targetcarriermodecode ")
    private String targetCarrierModeCode;

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
