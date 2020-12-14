package com.dappcraft.broxus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestTransfer {
    public String id;
    public Double value;
    public String currency;
    public String fromAddressType;
    public String fromUserAddress;
    public String fromWorkspaceId;
    public String toAddressType;
    public String toUserAddress;
    public String toWorkspaceId;
    public String applicationId;
}
