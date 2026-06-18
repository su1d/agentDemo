package com.agent.model;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    private String message;
    private String action;
    private List<String> roles;
    private Integer rounds;
    private String generateRole;
    private String critiqueRole;
    private Integer refineRounds;

    public ChatRequest() {}

    public ChatRequest(String message) { this.message = message; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public Integer getRounds() { return rounds; }
    public void setRounds(Integer rounds) { this.rounds = rounds; }

    public String getGenerateRole() { return generateRole; }
    public void setGenerateRole(String generateRole) { this.generateRole = generateRole; }

    public String getCritiqueRole() { return critiqueRole; }
    public void setCritiqueRole(String critiqueRole) { this.critiqueRole = critiqueRole; }

    public Integer getRefineRounds() { return refineRounds; }
    public void setRefineRounds(Integer refineRounds) { this.refineRounds = refineRounds; }
}
