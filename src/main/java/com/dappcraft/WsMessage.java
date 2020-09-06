package com.dappcraft;

public class WsMessage {
    private String type;
    private String cmd;
    private double[] quat;

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
}
