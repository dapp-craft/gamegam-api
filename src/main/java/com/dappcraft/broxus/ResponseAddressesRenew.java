package com.dappcraft.broxus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseAddressesRenew {
    public String currency;
    public String addressType;
    public String userAddress;
    public String workspaceId;
    public String blockchainAddress;
    public String createdAt;
}
