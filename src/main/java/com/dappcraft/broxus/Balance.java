package com.dappcraft.broxus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Balance {
    public String addressType;
    public String userAddress;
    public String workspaceId;
    public String currency;
    public Double total;
    public Double frozen;
    public Double available;
}
