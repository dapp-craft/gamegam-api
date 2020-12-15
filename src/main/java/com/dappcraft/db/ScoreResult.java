package com.dappcraft.db;

public class ScoreResult {
    private String userName;
    private Long level;
    private Long score;
    private Long kills;

    public ScoreResult(Long score, Long level, Long kills) {
        this.score = score;
        this.level = level;
        this.kills = kills;
    }

    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public Long getKills() {
        return kills;
    }

    public void setKills(Long kills) {
        this.kills = kills;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
