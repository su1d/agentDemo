package com.agent.service;

import com.agent.entity.SysProviderConfig;
import com.agent.repository.SysProviderConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provider configuration backed by database (sys_provider_config table).
 * Falls back to environment variables on first run (seeded by DataInitializer).
 */
@Service
public class ProviderConfigService {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfigService.class);

    private final SysProviderConfigRepository repository;

    public ProviderConfigService(SysProviderConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * Get API key for a provider, falling back to OPENAI_API_KEY env var.
     */
    @Cacheable(value = "providers", key = "'apiKey:' + #providerKey", unless = "#result == null")
    public String getApiKey(String providerKey) {
        return repository.findByProviderKey(providerKey)
            .filter(SysProviderConfig::isEnabled)
            .map(c -> {
                String key = c.getApiKey();
                return key != null && !key.isEmpty() ? key : null;
            })
            .orElse(System.getenv("OPENAI_API_KEY"));
    }

    /**
     * Get base URL for a provider.
     */
    @Cacheable(value = "providers", key = "'baseUrl:' + #providerKey", unless = "#result == null")
    public String getBaseUrl(String providerKey) {
        return repository.findByProviderKey(providerKey)
            .map(SysProviderConfig::getBaseUrl)
            .filter(s -> s != null && !s.isEmpty())
            .orElse("https://api.openai.com/v1");
    }

    /**
     * Get default model for a provider.
     */
    @Cacheable(value = "providers", key = "'defaultModel:' + #providerKey", unless = "#result == null")
    public String getDefaultModel(String providerKey) {
        return repository.findByProviderKey(providerKey)
            .map(SysProviderConfig::getDefaultModel)
            .filter(s -> s != null && !s.isEmpty())
            .orElse("gpt-4o-mini");
    }

    /**
     * Get the primary enabled provider key (first enabled with API key).
     */
    @Cacheable(value = "providers", key = "'primaryKey'", unless = "#result == null")
    public String getPrimaryProviderKey() {
        return repository.findAll().stream()
            .filter(c -> c.isEnabled() && c.getApiKey() != null && !c.getApiKey().isEmpty())
            .map(SysProviderConfig::getProviderKey)
            .findFirst()
            .orElse("openai");
    }

    /**
     * Get all providers (API key masked for frontend display).
     */
    @Cacheable(value = "providers", key = "'allProviders'")
    public List<Map<String, Object>> getAllProviders() {
        return repository.findAll().stream()
            .map(this::toProviderMap)
            .collect(Collectors.toList());
    }

    /**
     * Update or create a provider configuration.
     */
    @Transactional
    @CacheEvict(value = "providers", allEntries = true)
    public void updateProvider(String key, String apiKey, String baseUrl, String defaultModel, boolean enabled) {
        SysProviderConfig config = repository.findByProviderKey(key).orElse(null);
        if (config == null) {
            config = new SysProviderConfig(key, key, apiKey, baseUrl, defaultModel, "", enabled);
        } else {
            config.setApiKey(apiKey);
            config.setBaseUrl(baseUrl);
            config.setDefaultModel(defaultModel);
            config.setEnabled(enabled);
        }
        repository.save(config);
        log.info("Provider '{}' configuration saved to database", key);
    }

    private Map<String, Object> toProviderMap(SysProviderConfig c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", c.getProviderKey());
        map.put("name", c.getName());
        map.put("apiKey", maskApiKey(c.getApiKey()));
        map.put("baseUrl", c.getBaseUrl());
        map.put("defaultModel", c.getDefaultModel());
        map.put("models", parseModels(c.getModels()));
        map.put("enabled", c.isEnabled());
        return map;
    }

    private List<String> parseModels(String modelsStr) {
        if (modelsStr == null || modelsStr.isBlank()) {
            return List.of("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo");
        }
        return Arrays.asList(modelsStr.split(","));
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
