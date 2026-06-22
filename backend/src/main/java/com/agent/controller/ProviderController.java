package com.agent.controller;

import com.agent.service.ProviderConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderController {

    private final ProviderConfigService providerConfigService;

    public ProviderController(ProviderConfigService providerConfigService) {
        this.providerConfigService = providerConfigService;
    }

    /**
     * Get all LLM providers with their configuration.
     */
    @GetMapping
    public List<Map<String, Object>> getAllProviders() {
        return providerConfigService.getAllProviders();
    }

    /**
     * Update a provider's configuration.
     */
    @PutMapping("/{key}")
    public Map<String, Object> updateProvider(
            @PathVariable String key,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String apiKey = (String) body.getOrDefault("apiKey", "");
            String baseUrl = (String) body.getOrDefault("baseUrl", "");
            String defaultModel = (String) body.getOrDefault("defaultModel", "");
            boolean enabled = body.containsKey("enabled") ? (boolean) body.get("enabled") : true;

            providerConfigService.updateProvider(key, apiKey, baseUrl, defaultModel, enabled);
            result.put("success", true);
            result.put("message", "Provider '" + key + "' updated");
        } catch (Exception e) {
            result.put("error", "Failed to update provider: " + e.getMessage());
        }
        return result;
    }
}
