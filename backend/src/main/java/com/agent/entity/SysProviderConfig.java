package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * LLM provider configuration persisted in database.
 */
@Entity
@Table(name = "sys_provider_config")
public class SysProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_key", nullable = false, unique = true, length = 50)
    private String providerKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "default_model", length = 100)
    private String defaultModel;

    @Column(name = "models", length = 1000)
    private String models;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public SysProviderConfig() {}

    public SysProviderConfig(String providerKey, String name, String apiKey, String baseUrl,
                             String defaultModel, String models, boolean enabled) {
        this.providerKey = providerKey;
        this.name = name;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.models = models;
        this.enabled = enabled;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderKey() { return providerKey; }
    public void setProviderKey(String providerKey) { this.providerKey = providerKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }

    public String getModels() { return models; }
    public void setModels(String models) { this.models = models; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
