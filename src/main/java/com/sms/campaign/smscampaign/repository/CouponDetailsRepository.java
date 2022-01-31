package com.sms.campaign.smscampaign.repository;

import com.sms.campaign.smscampaign.entity.CouponDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponDetailsRepository extends JpaRepository<CouponDetails,Integer > {

    public CouponDetails findByCouponCode(String coupon);

    public List<CouponDetails> findByStatusNot(int status);
    public List<CouponDetails> findByStatus(int status);

    public List<CouponDetails> findByCouponType(String couponType);


}
