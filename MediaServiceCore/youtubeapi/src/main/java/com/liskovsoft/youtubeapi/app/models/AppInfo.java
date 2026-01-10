package com.liskovsoft.youtubeapi.app.models;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.googlecommon.common.converters.regexp.RegExp;
import com.liskovsoft.googlecommon.common.helpers.ServiceHelper;

 
public class AppInfo {
     
    @RegExp("\"player_url\":\"(.*?)\"")
    private String mPlayerUrl;

     
    @RegExp({
            "id=\"base-js\" src=\"(.*?)\"",
            "\\.src = '(.*?m=base)'",  
            "\\.src = '(.*?)'; .\\.id = 'base-js'"})  
    private String mClientUrl;

     
    @RegExp("\"visitorData\":\"(.*?)\"")
    private String mVisitorData;

    public String getPlayerUrl() {
        return ServiceHelper.tidyUrl(mPlayerUrl);
    }

    public String getClientUrl() {
        return ServiceHelper.tidyUrl(mClientUrl);
    }

    public String getVisitorData() {
        return mVisitorData;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppInfo) {
            AppInfo target = (AppInfo) obj;
            return Helpers.equals(getPlayerUrl(), target.getPlayerUrl()) &&
                    Helpers.equals(getClientUrl(), target.getClientUrl()) &&
                    Helpers.equals(getVisitorData(), target.getVisitorData());
        }

        return super.equals(obj);
    }
}
