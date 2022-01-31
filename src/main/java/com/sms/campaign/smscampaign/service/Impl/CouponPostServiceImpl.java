package com.sms.campaign.smscampaign.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.campaign.smscampaign.common.CommonConstants;
import com.sms.campaign.smscampaign.entity.ARVcoupondata;
import com.sms.campaign.smscampaign.entity.CouponDetails;
import com.sms.campaign.smscampaign.feign.client.PaytmFeignClient;
import com.sms.campaign.smscampaign.model.SmsRequest;
import com.sms.campaign.smscampaign.model.SmsRequestChange;
import com.sms.campaign.smscampaign.model.SmsResponse;
import com.sms.campaign.smscampaign.model.SmsResponseChange;
import com.sms.campaign.smscampaign.repository.ARVcoupondatarRepository;
import com.sms.campaign.smscampaign.repository.CouponDetailsRepository;
import com.sms.campaign.smscampaign.service.CouponPostService;
import com.sms.campaign.smscampaign.validation.DataValidatior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

//import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class CouponPostServiceImpl implements CouponPostService {
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
    public void sendData() throws Exception {
        List<ARVcoupondata>   custDataPending = arVcoupondatarRepository.getPendingCustomerData();
        List<ARVcoupondata> invalidCustData = new ArrayList<>();
        for (ARVcoupondata cust : custDataPending) {
            getLogger().info("Start Campaign ");

            if(!DataValidatior.isValidMobileNo(cust.getMobileNo())){
                cust.setStatus("Invalid Mobile No");
                invalidCustData.add(cust);
            }else{
               // List<CouponDetails> listCoupons = couponDetailsRepository.findByStatusNot(1);// send coupon data that have status 0
                List<CouponDetails> listCoupons = couponDetailsRepository.findByStatus(0);
                if (listCoupons.isEmpty()) {
                    getLogger().info(" Coupon is not available ");
                    return;
                }
                CouponDetails couponDetails = null;
                SmsRequestChange smsRequest = null;
                if (CommonConstants.brandUspa.equalsIgnoreCase(cust.getId().getBrand())) {
                    couponDetails = getCouponDetailByBrandAndAmount(listCoupons, cust);
                    if (null != couponDetails) {
                        smsRequest = new SmsRequestChange();
                        smsRequest.setPe_id(env.getProperty("peId.uspa"));
                        smsRequest.setSender(env.getProperty("senderId.uspa"));
                        smsRequest.setTemplate_id(env.getProperty("templateId.uspa"));

                        String messageContent = env.getProperty("message.template.uspa")
                                .replaceAll("couponCode", couponDetails.getCouponCode())
                                .replaceAll("giftName", couponDetails.getCouponType())
                                .replaceAll("redeemUrl", couponDetails.getCouponType().equalsIgnoreCase(CommonConstants.gift_jbl_in_hp) ?
                                        env.getProperty("url.jbl.in.earphone") : env.getProperty("url.jbl.on.earphone") )
                                .replaceAll("endDate", env.getProperty("uspa.campaign.end.date"));
                        smsRequest.setContent(messageContent);
                        smsRequest.setReceiver(cust.getMobileNo());
                        String ref_id = getUniqueRefNumber();
                        smsRequest.setRef_id(ref_id);
                        String req_time =getReqTime();
                        smsRequest.setReq_time(req_time);
                        smsRequest.setMsg_type("TEXT");
                        getLogger().info("SMS Request "+smsRequest.toString());
                    }else{
                        cust.setSmsMessage("Coupon Not available");
                        invalidCustData.add(cust);
                    }

                } else if (CommonConstants.brandFyingMachine.equalsIgnoreCase(cust.getId().getBrand())) {
                    couponDetails = getCouponDetailByBrandAndAmount(listCoupons, cust);
                    if (null != couponDetails) {
                        smsRequest = new SmsRequestChange();
                        smsRequest.setPe_id(env.getProperty("peId.fm"));
                        smsRequest.setSender(env.getProperty("senderId.fm"));
                        smsRequest.setTemplate_id(env.getProperty("templateId.fm"));
                        String messageContent = env.getProperty("message.template.fm")
                                .replaceAll("couponCode", couponDetails.getCouponCode())
                                .replaceAll("giftName", couponDetails.getCouponType())
                                .replaceAll("redeemUrl", env.getProperty("url.boat.smartwatch"))
                                .replaceAll("endDate", env.getProperty("fm.campaign.end.date"));
                        smsRequest.setContent(messageContent);
                        smsRequest.setReceiver(cust.getMobileNo());
                        String ref_id = getUniqueRefNumber();
                        smsRequest.setRef_id(ref_id);
                        String req_time =getReqTime();
                        smsRequest.setReq_time(req_time);
                        smsRequest.setMsg_type("TEXT");
                        getLogger().info(" SMS Request Content:{} ",messageContent);
                    }else{
                        cust.setSmsMessage("Coupon Not available");
                        invalidCustData.add(cust);
                        getLogger().info("Coupon Not available ");
                    }

                }

                ObjectMapper mapper = new ObjectMapper();
                if(null != smsRequest){
                    try{
                        waitForSmsSend(5000);
                        String smsResponseString = sendSmsToCustomer(smsRequest);
                        getLogger().info("Response from paytm feign client", smsResponseString);
                        SmsResponseChange smsResponse = mapper.readValue(smsResponseString, SmsResponseChange.class);
                        if(null != smsResponse &&
                                smsResponse.getSTATUS_CODE().equalsIgnoreCase("202")){
                            getLogger().info(" Sms Response : ",smsResponse.getRESPONSE());
                            if(null!= smsResponse && smsResponse.getSTATUS_CODE().equalsIgnoreCase("202")){
                                postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "SENT", smsRequest.getContent());
                            }else{
                                postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "FAIL", "Internal Error");
                            }
                        }
                    }catch (Exception ex){
                        logger.error(ex.getMessage(), ex);
                        postCustomerAndCouponData(cust, couponDetails.getCouponCode(), "FAIL", ex.getMessage());
                    }
                }
            }
            getLogger().info("End Campaign ");
        }

        if(!invalidCustData.isEmpty()){
            try {
                arVcoupondatarRepository.saveAll(invalidCustData);
            } catch(Exception ex){
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void reSentSms() throws Exception {

        List<ARVcoupondata>   listOfReSentSms =null ;//; arVcoupondatarRepository.findByDelivered(0);
        for(ARVcoupondata resmscust:listOfReSentSms){
            getLogger().info("Start Resent Campaign");
            if(!DataValidatior.isValidMobileNo(resmscust.getMobileNo())){
                resmscust.setStatus("Invalid Mobile No");

            }else{

                    SmsRequestChange smsRequest = null;
                    if (CommonConstants.brandUspa.equalsIgnoreCase(resmscust.getId().getBrand())) {
                        if(resmscust.getGross_amt()>=CommonConstants.Slab2){
                            smsRequest = new SmsRequestChange();
                            smsRequest.setPe_id(env.getProperty("peId.uspa"));
                            smsRequest.setSender(env.getProperty("senderId.uspa"));
                            smsRequest.setTemplate_id(env.getProperty("templateId.uspa.resent.in"));

                            String messageContent = env.getProperty("message.template.resent.uspa")
                                    .replaceAll("couponCode", resmscust.getCouponCode())
                                    .replaceAll("giftName", CommonConstants.gift_jbl_in_hp)
                                    .replaceAll("redeemUrl",env.getProperty("url.jbl.in.earphone") )
                                    .replaceAll("endDate", env.getProperty("uspa.campaign.end.date"));
                            smsRequest.setContent(messageContent);
                            smsRequest.setReceiver(resmscust.getMobileNo());
//                            smsRequest.setRef_id();

                            getLogger().info("SMS Request "+smsRequest.toString());
                            getLogger().info("Resent Messsage :{}",messageContent);
                            getLogger().info("Gross Amt :{} "+resmscust.getGross_amt()+" Coupon Code :{1}"+resmscust.getCouponCode()+" Mobile No :{2}"+resmscust.getMobileNo());

                        }else if(resmscust.getGross_amt()>=CommonConstants.Slab1
                                && resmscust.getGross_amt()< CommonConstants.Slab2){
                            smsRequest = new SmsRequestChange();
                            smsRequest.setPe_id(env.getProperty("peId.uspa"));
                            smsRequest.setSender(env.getProperty("senderId.uspa"));
                            smsRequest.setTemplate_id(env.getProperty("templateId.uspa.resent.on"));

                            String messageContent = env.getProperty("message.template.resent.uspa")
                                    .replaceAll("couponCode", resmscust.getCouponCode())
                                    .replaceAll("giftName", CommonConstants.gift_jbl_on_hp)
                                    .replaceAll("redeemUrl",env.getProperty("url.jbl.on.earphone") )
                                    .replaceAll("endDate", env.getProperty("uspa.campaign.end.date"));
                            smsRequest.setContent(messageContent);
                            smsRequest.setReceiver(resmscust.getMobileNo());
                            getLogger().info("SMS Request "+smsRequest.toString());
                            getLogger().info("Resent Messsage :{}",messageContent);
                            getLogger().info("Gross Amt :{} "+resmscust.getGross_amt()+" Coupon Code :{1}"+resmscust.getCouponCode()+" Mobile No :{2}"+resmscust.getMobileNo());
                        }
                    } else if (CommonConstants.brandFyingMachine.equalsIgnoreCase(resmscust.getId().getBrand())) {
                                 if(resmscust.getGross_amt()>=CommonConstants.Slab1){
                                     smsRequest = new SmsRequestChange();
                                     smsRequest.setPe_id(env.getProperty("peId.fm"));
                                     smsRequest.setSender(env.getProperty("senderId.fm"));
                                     smsRequest.setTemplate_id(env.getProperty("templateId.resent.fm"));
                                     String messageContent = env.getProperty("message.template.resent.fm")
                                             .replaceAll("couponCode", resmscust.getCouponCode())
                                             .replaceAll("giftName", CommonConstants.gift_boat_sw)
                                             .replaceAll("redeemUrl", env.getProperty("url.boat.smartwatch"))
                                             .replaceAll("endDate", env.getProperty("fm.campaign.end.date"));
                                     smsRequest.setContent(messageContent);
                                     smsRequest.setReceiver(resmscust.getMobileNo());
                                     getLogger().info(" SMS Request Content:{} ",messageContent);
                                     getLogger().info("SMS Request "+smsRequest.toString());
                                     getLogger().info("Resent Messsage :{}",messageContent);
                                     getLogger().info("Gross Amt :{} "+resmscust.getGross_amt()+" Coupon Code :{1}"+resmscust.getCouponCode()+" Mobile No :{2}"+resmscust.getMobileNo());
                                 }
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    if(null != smsRequest){
                        try{
                            waitForSmsSend(5000);
                            String smsResponseString = sendSmsToCustomer(smsRequest);
                            getLogger().info("Response from paytm feign client", smsResponseString);
                            SmsResponseChange smsResponse = mapper.readValue(smsResponseString, SmsResponseChange.class);
                            getLogger().info(" SMS Response Status "+smsResponse.getSTATUS_CODE());
                            getLogger().info(" SMS Response msgId "+smsResponse.getMESSAGE_ID());
                            if(null != smsResponse &&
                                    smsResponse.getSTATUS_CODE().equalsIgnoreCase("202")){
                                getLogger().info(" Sms Response : ",smsResponse.getRESPONSE());
                                if(null!= smsResponse && smsResponse.getSTATUS_CODE().equalsIgnoreCase("202")){
                                   // resmscust.setDelivered(1);
                                    resmscust.setSmsMessage(smsRequest.getContent());
                                    resmscust.setCouponApplyDate(new Date(System.currentTimeMillis()));
                                    arVcoupondatarRepository.save(resmscust);

                                }
                            }
                        }catch (Exception ex){
                            logger.error(ex.getMessage(), ex);
                            resmscust.setSmsMessage(ex.getMessage());
                            arVcoupondatarRepository.save(resmscust);
                        }
                    }


            }
            getLogger().info("End Resent Campaign");
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


    private String sendSmsToCustomer(SmsRequestChange smsRequest) throws  Exception{
        getLogger().error(" SMS Request by sendSmsToCustomer :{}",smsRequest.toString());
        return paytmFeignClient.sendSmsByPaytm(env.getProperty("paytm.bulk.sms.api.username"),
                env.getProperty("paytm.bulk.sms.api.password"),
                env.getProperty("paytm.bulk.sms.api.AccessToken"),
                "1",
                smsRequest.toString());

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
            c.setRtlLocId(cust.getId().getRtlLocId());
            c.setTransSeq(cust.getId().getTransSeq());
            couponDetailsRepository.save(c);
            getLogger().info("Coupon data to Save :{} ",coupon);
        }else{
            cust.setStatus(status);
            cust.setSmsMessage(message);
            arVcoupondatarRepository.save(cust);
        }
    }

    private void waitForSmsSend(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.err.format("IOException: %s%n", e);
        }
    }

//    public static String getCurrentDateTimeMS() {
//        Date dNow = new java.util.Date();
//        SimpleDateFormat ft = new SimpleDateFormat("yyMMddhhmmssMs");
//        String datetime = ft.format(dNow);
//        return datetime;
//    }

    private static synchronized String getUniqueRefNumber()
    {

        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddHHmmssSS");
        String datetime = ft.format(dNow);
        try
        {
            Thread.sleep(1);
        }catch(Exception e)
        {

        }
        return datetime;

    }

    private static String getReqTime(){
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        String strDate = dateFormat.format(date);
        System.out.println("Converted String: " + strDate);

        return strDate;
    }

}

