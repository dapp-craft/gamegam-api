package com.dappcraft;

import java.util.List;
import java.util.Map;

public class WsMessage {
    private String type;
    private String cmd;
    private Long timestamp;
    private String pin;
    private double[] quat;
    private Map<String, String> data;
    private Integer level;
    private Integer score;
    private Integer kills;
    private List<ScoreResult>  scoreTable;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public double[] getQuat() {
        return quat;
    }

    public void setQuat(double[] quat) {
        this.quat = quat;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getKills() {
        return kills;
    }

    public void setKills(Integer kills) {
        this.kills = kills;
    }

    public String getUserName() {
        if (this.getData() != null && this.getData().containsKey("name"))
            return this.getData().get("name");
        return "";
    }

    public void setScoreTable(List<ScoreResult> scoreTable) {
        this.scoreTable = scoreTable;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
