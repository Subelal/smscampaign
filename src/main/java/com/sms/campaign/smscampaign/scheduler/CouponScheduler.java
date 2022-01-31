package com.sms.campaign.smscampaign.scheduler;

import com.sms.campaign.smscampaign.service.CouponPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class CouponScheduler {

    @Autowired
    private CouponPostService service;

    @Scheduled(cron = "0 0 0/1 * * *")
    public void processCampaign() throws Exception {
        service.sendData();

    }

    @Scheduled(cron = "0 0/1 * * * *")
    public  void testScheduler() throws Exception {
        System.out.println("Testing scheduler cron...");

    }

}
