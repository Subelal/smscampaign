package com.sms.campaign.smscampaign.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.campaign.smscampaign.common.CommonConstants;
import com.sms.campaign.smscampaign.entity.ARVcoupondata;
import com.sms.campaign.smscampaign.entity.CouponDetails;
import com.sms.campaign.smscampaign.feign.client.PaytmFeignClient;
import com.sms.campaign.smscampaign.model.SmsRequest;
import com.sms.campaign.smscampaign.model.SmsResponse;
import com.sms.campaign.smscampaign.repository.ARVcoupondatarRepository;
import com.sms.campaign.smscampaign.repository.CouponDetailsRepository;
import com.sms.campaign.smscampaign.service.TestSmsSendService;
import com.sms.campaign.smscampaign.validation.DataValidatior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestSmsSendImpl  implements TestSmsSendService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Environment env;

    @Autowired
    private CouponDetailsRepository couponDetailsRepository;

    @Autowired
    private PaytmFeignClient paytmFeignClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private DataValidatior mobileNumberValidation;
    @Autowired
    private ARVcoupondatarRepository arVcoupondatarRepository;


    public Logger getLogger() {
        return logger;
    }

    @Override
    public void sendSms() throws Exception {
        List<ARVcoupondata>   custDataPending = arVcoupondatarRepository.getPendingCustomerData();
        List<ARVcoupondata> invalidCustData = new ArrayList<>();
        for (ARVcoupondata cust : custDataPending) {


            if(!DataValidatior.isValidMobileNo(cust.getMobileNo())){
                cust.setStatus("Invalid");
                invalidCustData.add(cust);
            }else{
                // List<CouponDetails> listCoupons = couponDetailsRepository.findByStatusNot(1);// send coupon data that have status 0
                List<CouponDetails> listCoupons = couponDetailsRepository.findByStatus(0);
                if (listCoupons.isEmpty()) {
                    getLogger().info(" Coupon is not available ");
                    return;
                }
                CouponDetails couponDetails = null;
                SmsRequest smsRequest = null;
                if (CommonConstants.brandUspa.equalsIgnoreCase(cust.getId().getBrand())) {
                    getLogger().info("Start Campaign USPA");
                    couponDetails = getCouponDetailByBrandAndAmount(listCoupons, cust);
                    if (null != couponDetails) {
                        smsRequest = new SmsRequest();
                        smsRequest.setPe_id(env.getProperty("peId.uspa"));
                        smsRequest.setFrom(env.getProperty("senderId.uspa"));
                        smsRequest.setTemplate_id(env.getProperty("templateId.uspa"));

                        String messageContent = env.getProperty("message.template.uspa")
                                .replaceAll("couponCode", couponDetails.getCouponCode())
                                .replaceAll("giftName", couponDetails.getCouponType())
                                .replaceAll("redeemUrl", couponDetails.getCouponType().equalsIgnoreCase(CommonConstants.gift_jbl_in_hp) ?
                                        env.getProperty("url.jbl.in.earphone") : env.getProperty("url.jbl.on.earphone") )
                                .replaceAll("endDate", env.getProperty("uspa.campaign.end.date"));
                        smsRequest.setContent(messageContent);
                        smsRequest.setReceiver(cust.getMobileNo());
                     //   postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "SENT", smsRequest.getContent());
                        getLogger().info("SMS Request "+smsRequest.toString());
                        getLogger().info("End campaign USPA");
                    }else{
                        cust.setSmsMessage("Coupon Not available");
                        invalidCustData.add(cust);
                    //    postCustomerAndCouponData(cust, "Coupon Not available", "FAIL", "Internal Error");
                    }

                } else if (CommonConstants.brandFyingMachine.equalsIgnoreCase(cust.getId().getBrand())) {
                    getLogger().info("Start campaign FLYING MACHINE");
                    couponDetails = getCouponDetailByBrandAndAmount(listCoupons, cust);
                    if (null != couponDetails) {
                        smsRequest = new SmsRequest();
                        smsRequest.setPe_id(env.getProperty("peId.fm"));
                        smsRequest.setFrom(env.getProperty("senderId.fm"));
                        smsRequest.setTemplate_id(env.getProperty("templateId.fm"));
                        String messageContent = env.getProperty("message.template.fm")
                                .replaceAll("couponCode", couponDetails.getCouponCode())
                                .replaceAll("giftName", couponDetails.getCouponType())
                                .replaceAll("redeemUrl", env.getProperty("url.boat.smartwatch"))
                                .replaceAll("endDate", env.getProperty("fm.campaign.end.date"));
                        smsRequest.setContent(messageContent);
                        smsRequest.setReceiver(cust.getMobileNo());
                        getLogger().info(" SMS Request Content:{} ",messageContent);
                        getLogger().info("End campaign FLYING MACHINE ");
                       // postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "SENT", smsRequest.getContent());
                    }else{
                        cust.setSmsMessage("Coupon Not available");
                        invalidCustData.add(cust);
                        getLogger().info("Coupon Not available ");
                       // postCustomerAndCouponData(cust, "not elligible", "FAIL", "Internal Error");
                    }

                }

                ObjectMapper mapper = new ObjectMapper();
//                if(null != smsRequest){
//                    try{
//                        String smsResponseString = sendSmsToCustomer(smsRequest);
//                        getLogger().info("Response from paytm feign client", smsResponseString);
//                        SmsResponse smsResponse = mapper.readValue(smsResponseString, SmsResponse.class);
//                        if(null != smsResponse &&
//                                smsResponse.getErrorCode().equalsIgnoreCase("0")){
//                            getLogger().info(" Sms Response : ",smsResponseString);
//                            if(null!= smsResponse && smsResponse.getErrorCode().equalsIgnoreCase("0")){
//                                postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "SENT", smsRequest.getContent());
//                            }else{
//                                postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "FAIL", "Internal Error");
//                            }
//                        }
//                    }catch (Exception ex){
//                        logger.error(ex.getMessage(), ex);
//                        postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "FAIL", ex.getMessage());
//                    }
//                }
            }
        }

        if(!invalidCustData.isEmpty()){
            try {
                arVcoupondatarRepository.saveAll(invalidCustData);
            } catch(Exception ex){
                logger.error(ex.getMessage(), ex);
            }
        }
    }
    private CouponDetails getCouponDetailByBrandAndAmount(List<CouponDetails> listCoupons, ARVcoupondata cust) {

        CouponDetails couponDetail =null;
        getLogger().info("Brand information :{} Gross Amount :{} ",cust.getId().getBrand(),cust.getGross_amt());
        if (CommonConstants.brandUspa.equalsIgnoreCase(cust.getId().getBrand())
                && cust.getGross_amt() >= CommonConstants.Slab2) {
            couponDetail = filterCouponDetail(listCoupons,cust.getId().getBrand(), CommonConstants.gift_jbl_in_hp);
            getLogger().info("Coupon Details :{}"+ couponDetail+" slab2 "+ CommonConstants.Slab2+" Gift JBL in Hp "+CommonConstants.gift_jbl_on_hp+ " Brand "+CommonConstants.brandUspa);
        }else if(CommonConstants.brandUspa.equalsIgnoreCase(cust.getId().getBrand())
                && cust.getGross_amt() >= CommonConstants.Slab1){
            couponDetail = filterCouponDetail(listCoupons,cust.getId().getBrand(), CommonConstants.gift_jbl_on_hp);
            getLogger().info("Coupon Details :{}"+ couponDetail+" slab1 "+ CommonConstants.Slab1+" Gift JBL on Hp "+CommonConstants.gift_jbl_on_hp+ " Brand "+CommonConstants.brandUspa);
        } else if (CommonConstants.brandFyingMachine.equalsIgnoreCase(cust.getId().getBrand())
                && cust.getGross_amt() >= CommonConstants.Slab1){
            couponDetail = filterCouponDetail(listCoupons,cust.getId().getBrand(), CommonConstants.gift_boat_sw);
            getLogger().info("Coupon Details :{}"+ couponDetail+" slab1 "+ CommonConstants.Slab1+" Gift boat sw  "+CommonConstants.gift_boat_sw+ " Brand "+CommonConstants.brandFyingMachine);
        }
        return couponDetail;
    }

    private CouponDetails filterCouponDetail(List<CouponDetails> listCoupons, String brand, String giftName) {

        CouponDetails couponDetail = null;
        for(CouponDetails cd : listCoupons){
            if(cd.getBrand().equalsIgnoreCase(brand)
                    && cd.getCouponType().equalsIgnoreCase(giftName)){
                couponDetail = cd;
                getLogger().info(" Filter Coupon Details "+couponDetail+" Brand "+cd.getBrand()+" Gift Name "+giftName);
                break;
            }
        }
        return couponDetail;
    }

//    private String getCoupon(String couponType){
//
//        String coupon = null;
//        List<CouponDetails> ctype= couponDetailsRepository.findByCouponType(couponType);
//        if(null != ctype && !ctype.isEmpty()){
//            coupon = ctype.get(0).getCouponCode();
//        }
//
//        return coupon;
//    }


    private String sendSmsToCustomer1(SmsRequest smsRequest) throws  Exception{
        getLogger().error(" SMS Request by sendSmsToCustomer :{}",smsRequest.toString());
        return paytmFeignClient.sendSmsByPaytm(env.getProperty("paytm.bulk.sms.api.username"),
                env.getProperty("paytm.bulk.sms.api.password"), env.getProperty("paytm.bulk.sms.api.AccessToken"),
                "1",
                smsRequest.toString());

    }
    private String sendSmsToCustomer(SmsRequest smsRequest) throws  Exception{
        getLogger().error(" SMS Request by sendSmsToCustomer :{}",smsRequest.toString());

        return "send";

    }


    private void postCustomerAndCouponData(ARVcoupondata cust, String coupon, String status, String message) {
        getLogger().info(" Customer data :{} Coupon Data :{} Status :{} Message :{}  ",cust,coupon,status,message);

        if("SENT".equalsIgnoreCase(status)){
            cust.setStatus(status);
            cust.setSmsMessage(message);
            cust.setCouponCode(coupon);
            cust.setCouponApplyDate(new Date(System.currentTimeMillis()));
            arVcoupondatarRepository.save(cust);
            getLogger().info("Cust data to Save :{} ",cust);
            CouponDetails c = couponDetailsRepository.findByCouponCode(coupon);
            c.setStatus(1);
            c.setCouponApplyDate(new Date(System.currentTimeMillis()));
            c.setMobileNo(cust.getMobileNo());
            c.setGross_amt(cust.getGross_amt());
            couponDetailsRepository.save(c);
            getLogger().info("Coupon data to Save :{} ",coupon);
        }else{
            cust.setStatus(status);
            cust.setSmsMessage(message);
            arVcoupondatarRepository.save(cust);
        }
    }
}


