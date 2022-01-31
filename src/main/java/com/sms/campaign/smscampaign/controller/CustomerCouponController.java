package com.sms.campaign.smscampaign.controller;


import com.sms.campaign.smscampaign.service.CouponPostService;
import com.sms.campaign.smscampaign.service.TestSmsSendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerCouponController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CouponPostService service;

    @Autowired
    private TestSmsSendService testSmsSendService;

//    @GetMapping("/start/campaign")
//    public String processCampaign() throws Exception {
//        service.sendData();
//        return "Success";
//    }
//    @GetMapping("/start/smsSent")
//    public String processSmsSent() throws Exception{
//        testSmsSendService.sendSms();
//        return "Success Send SMS";
//    }

//    @GetMapping("/start/resentsms")
//    public String processReSent() throws Exception{
//        service.reSentSms();
//
//        return "Resent SMS to Customer";
//    }

    @GetMapping("/hello")
    public String hello() {

        logger.info("Service test : info");
        logger.error("Service test : error");
        logger.debug("Service test : debug");
        return "Hello Test";
    }

}
