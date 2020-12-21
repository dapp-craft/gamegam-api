package com.dappcraft.db;

import java.util.Date;

public class UserInfo {
    private String userName;
    private String telegramName;
    private String telegramId;
    private String questStep;
    private String claimTransactionId;
    private Double reward;
    private Boolean claimApproved;
    private Boolean rewardClaimed;
    private Boolean hasClaimedName;
    private Boolean joinGroup;
    private Date connectDate;
    private Date claimDate;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTelegramName() {
        return telegramName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }

    public String getQuestStep() {
        return questStep;
    }

    public void setQuestStep(String questStep) {
        this.questStep = questStep;
    }

    public Double getReward() {
        return reward;
    }

    public void setReward(Double reward) {
        this.reward = reward;
    }

    public boolean getRewardClaimed() {
        return rewardClaimed != null?rewardClaimed:false;
    }

    public void setRewardClaimed(Boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }

    public boolean getJoinGroup() {
        return joinGroup != null?joinGroup:false;
    }

    public void setJoinGroup(Boolean joinGroup) {
        this.joinGroup = joinGroup;
    }

    public Date getConnectDate() {
        return connectDate;
    }

    public void setConnectDate(Date connectDate) {
        this.connectDate = connectDate;
    }

    public Date getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(Date claimDate) {
        this.claimDate = claimDate;
    }

    public String getClaimTransactionId() {
        return claimTransactionId;
    }

    public void setClaimTransactionId(String claimTransactionId) {
        this.claimTransactionId = claimTransactionId;
    }

    public boolean getClaimApproved() {
        return claimApproved != null? claimApproved :false;
    }

    public void setClaimApproved(Boolean claimApproved) {
        this.claimApproved = claimApproved;
    }

    public boolean getHasClaimedName() {
        return hasClaimedName!= null?hasClaimedName:false;
    }

    public void setHasClaimedName(Boolean hasClaimedName) {
        this.hasClaimedName = hasClaimedName;
    }
}
