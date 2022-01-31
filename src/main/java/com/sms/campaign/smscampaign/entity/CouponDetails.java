package com.sms.campaign.smscampaign.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="Arv_coupon_detail")
public class CouponDetails {

    @Id
    @GeneratedValue
    private long id;
    @Column(unique=true)
    private String couponCode;
    private String couponType;
    private int status;
    private String brand;
    @Column(name = "COUPON_APPLY_DATE", columnDefinition = "TIMESTAMP")
    private Date couponApplyDate;
    @Column(name="TELIPHONE_NUMBER")
    private String mobileNo;
    @Column(name = "GROSS_AMT",columnDefinition = "long default 0" ,nullable=false)
    private long gross_amt;
    @Column(name = "RTL_LOC_ID", nullable = false)
    private Long rtlLocId;

    @Column(name = "TRANS_SEQ", nullable = false)
    private Long transSeq;
}
