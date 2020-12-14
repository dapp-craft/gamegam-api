package com.dappcraft;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckResult {
    public Boolean result;
    public Long user_id;
}
