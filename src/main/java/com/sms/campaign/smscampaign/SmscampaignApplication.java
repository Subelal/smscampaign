package com.sms.campaign.smscampaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients

public class SmscampaignApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmscampaignApplication.class, args);
	}

}
