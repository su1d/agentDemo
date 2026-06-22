package com.agent.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared configuration for LLM providers.
 * Stores provider settings (API key, base URL, model, enabled flag) in memory.
 * Used by AgentWorkerService when starting Python workers.
 */
@Service
public class ProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);

    private final Map<String, ProviderInfo> providers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Default providers loaded from environment
        String openaiKey = System.getenv("OPENAI_API_KEY");
        String deepseekKey = System.getenv("DEEPSEEK_API_KEY");

        providers.put("openai", new ProviderInfo(
            "openai", "OpenAI", 
            openaiKey != null ? openaiKey : "",
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            List.of("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"),
            true
        ));

        providers.put("deepseek", new ProviderInfo(
            "deepseek", "DeepSeek",
            deepseekKey != null ? deepseekKey : "",
            "https://api.deepseek.com",
            "deepseek-chat",
            List.of("deepseek-chat", "deepseek-reasoner"),
            true
        ));

        log.info("ProviderConfig initialized: {} providers loaded", providers.size());
    }

    /**
     * Get the effective API key for a given provider.
     * Falls back to OPENAI_API_KEY env var for backward compatibility.
     */
    public String getApiKey(String providerKey) {
        ProviderInfo info = providers.get(providerKey);
        if (info != null && info.enabled && !info.apiKey.isEmpty()) {
            return info.apiKey;
        }
        // Fallback to environment variable
        return System.getenv("OPENAI_API_KEY");
    }

    /**
     * Get the effective base URL for a provider.
     */
    public String getBaseUrl(String providerKey) {
        ProviderInfo info = providers.get(providerKey);
        if (info != null) {
            return info.baseUrl;
        }
        return "https://api.openai.com/v1";
    }

    /**
     * Get the effective model name for a provider.
     */
    public String getDefaultModel(String providerKey) {
        ProviderInfo info = providers.get(providerKey);
        if (info != null) {
            return info.defaultModel;
        }
        return "gpt-4o-mini";
    }

    /**
     * Get the primary enabled provider key.
     */
    public String getPrimaryProviderKey() {
        return providers.entrySet().stream()
            .filter(e -> e.getValue().enabled && !e.getValue().apiKey.isEmpty())
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("openai");
    }

    /**
     * Get all providers (without exposing full API keys).
     */
    public List<Map<String, Object>> getAllProviders() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, ProviderInfo> entry : providers.entrySet()) {
            ProviderInfo info = entry.getValue();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", info.key);
            map.put("name", info.name);
            map.put("apiKey", maskApiKey(info.apiKey));
            map.put("baseUrl", info.baseUrl);
            map.put("defaultModel", info.defaultModel);
            map.put("models", info.models);
            map.put("enabled", info.enabled);
            result.add(map);
        }
        return result;
    }

    /**
     * Update a provider's configuration.
     */
    public void updateProvider(String key, String apiKey, String baseUrl, String defaultModel, boolean enabled) {
        ProviderInfo existing = providers.get(key);
        if (existing != null) {
            existing.apiKey = apiKey;
            existing.baseUrl = baseUrl;
            existing.defaultModel = defaultModel;
            existing.enabled = enabled;
            log.info("Provider '{}' configuration updated", key);
        }
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    public static class ProviderInfo {
        public String key;
        public String name;
        public String apiKey;
        public String baseUrl;
        public String defaultModel;
        public List<String> models;
        public boolean enabled;

        public ProviderInfo(String key, String name, String apiKey, String baseUrl,
                           String defaultModel, List<String> models, boolean enabled) {
            this.key = key;
            this.name = name;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.defaultModel = defaultModel;
            this.models = models;
            this.enabled = enabled;
        }
    }
}
