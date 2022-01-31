package com.sms.campaign.smscampaign.validation;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataValidatior {

    public static boolean isValidMobileNo(String mobileNo) {
        boolean isValid;
        if ((mobileNo == null) || (mobileNo.equals(""))) {
            isValid = false;
            return isValid;
        }
        Pattern ptrn = Pattern.compile("(0/91)?[1-9][0-9]{9}");
        Matcher match = ptrn.matcher(mobileNo);
        isValid = (match.find() && match.group().equals(mobileNo));
        return isValid;
    }
}
