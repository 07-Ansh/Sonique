package com.liskovsoft.googlecommon.common.models.auth;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;

 
public class UserCode {
     
    @JsonPath("$.device_code")
    private String mDeviceCode;

     
    @JsonPath("$.user_code")
    private String mUserCode;

    @JsonPath("$.expires_in")
    private int mExpiresIn;

     
    @JsonPath("$.interval")
    private int mInterval;
    
    @JsonPath("$.verification_url")
    private String mVerificationUrl;

    private String mUserCodePretty;

    public String getDeviceCode() {
        return mDeviceCode;
    }

    public String getUserCode() {
        if (mUserCode == null) {
            return null;
        }

         
        if (mUserCodePretty == null) {
            mUserCodePretty = mUserCode.replace("-", " ");
        }

        return mUserCodePretty;
    }

    public String getVerificationUrl() {
        return mVerificationUrl;
    }

    public int getInterval() {
        return mInterval;
    }

    public int getExpiresIn() {
        return mExpiresIn;
    }
}
