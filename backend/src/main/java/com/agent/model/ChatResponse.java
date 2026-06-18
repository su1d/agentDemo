package com.agent.model;

import java.util.List;
import java.util.Map;

public class ChatResponse {
    private String reply;
    private String mode;
    private Object detail;

    public ChatResponse() {}

    public ChatResponse(String reply) {
        this.reply = reply;
    }

    public ChatResponse(String reply, String mode, Object detail) {
        this.reply = reply;
        this.mode = mode;
        this.detail = detail;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Object getDetail() { return detail; }
    public void setDetail(Object detail) { this.detail = detail; }
}
