package com.dappcraft.db;

public class Prize {
    private Double amount;
    private Double probability;
    private Long initCount;
    private Long count;

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

    public Long getInitCount() {
        return initCount;
    }

    public void setInitCount(Long initCount) {
        this.initCount = initCount;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
