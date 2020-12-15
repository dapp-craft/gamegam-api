package com.dappcraft;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckResult {
    private Boolean result;
    public String user_id;

    public boolean getResult() {
        return result != null?result:false;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
}
