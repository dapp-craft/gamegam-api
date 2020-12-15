package com.dappcraft.db;

public class Prize {
    private Double amount;
    private Double probability;
    private Integer initCount;
    private Integer count;

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public Integer getInitCount() {
        return initCount;
    }

    public void setInitCount(Integer initCount) {
        this.initCount = initCount;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
