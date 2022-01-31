package com.sms.campaign.smscampaign.repository;

import com.sms.campaign.smscampaign.entity.ARVcoupondata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ARVcoupondatarRepository extends JpaRepository<ARVcoupondata, Long> {
    // List<ARVcoupondata> findById(ARVcoupondata coupondataID);
    List<ARVcoupondata> findByStatusIsNull();

     @Query(value = "Select * from ARV_coupon_data where coupon_code is null and status is null and trunc(business_date) >= to_date('15-09-21', 'dd-MM-yy')", nativeQuery = true)
  // @Query(value = "SELECT * FROM ARV_coupon_data WHERE coupon_code IS NULL AND STATUS IS NULL AND business_date >= '2021-09-15 00:00:00' ", nativeQuery = true)
    List<ARVcoupondata> getPendingCustomerData();

     //List<ARVcoupondata> findByDelivered(int delivered);
}

