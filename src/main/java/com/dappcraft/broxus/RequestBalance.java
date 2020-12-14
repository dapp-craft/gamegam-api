package com.dappcraft.broxus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestBalance {
    public String workspaceId;
}
