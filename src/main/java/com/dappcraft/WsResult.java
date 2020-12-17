package com.dappcraft;

import com.dappcraft.db.UserInfo;

public class WsResult {
    public String cmd;
    public Boolean success;
    public Boolean claimApproved;
    public Double reward;
    public String error;
    public UserInfo userInfo;
}
