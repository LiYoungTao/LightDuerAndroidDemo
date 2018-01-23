package com.baidu.lightduer.sample;

import com.google.gson.annotations.SerializedName;

/**
 * <p>
 * Created by lishun02 on 2017/9/14.
 */

public class LightDuerResultObj {
    @SerializedName("url")
    public String speechUrl;
    @SerializedName("extra")
    public Extra extra;

    public class Extra {
        @SerializedName("txt")
        public String queryText;
    }
}
