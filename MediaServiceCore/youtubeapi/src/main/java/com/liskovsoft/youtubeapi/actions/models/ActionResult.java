package com.liskovsoft.youtubeapi.actions.models;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;

public class ActionResult {
     
    @JsonPath("$.responseContext.serviceTrackingParams[0].service")
    private String mTrackingParams;

     
    @JsonPath("$.responseContext.visitorData")
    private String mVisitorData;
}
