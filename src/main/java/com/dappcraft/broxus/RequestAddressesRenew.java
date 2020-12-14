package com.dappcraft.broxus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestAddressesRenew {
    public String currency;
    public String addressType;
    public String userAddress;
    public String workspaceId;
}
