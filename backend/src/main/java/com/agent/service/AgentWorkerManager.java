package com.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple AgentWorkerService instances, one per role.
 * Automatically starts all workers on application startup.
 */
@Service
public class AgentWorkerManager {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkerManager.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, AgentWorkerService> workers = new ConcurrentHashMap<>();

    // All available agent roles
    private static final List<String> ROLES = List.of(
        "orchestrator", "calculator", "weather", "searcher", "summarizer"
    );

    @PostConstruct
    public void init() {
        for (String role : ROLES) {
            AgentWorkerService worker = new AgentWorkerService(role);
            worker.start();
            workers.put(role, worker);
        }
        log.info("All agent workers initialized: {}", ROLES);
    }

    @PreDestroy
    public void shutdown() {
        for (AgentWorkerService worker : workers.values()) {
            worker.stop();
        }
        workers.clear();
    }

    /**
     * Get a specific worker by role.
     */
    public AgentWorkerService getWorker(String role) {
        return workers.get(role);
    }

    /**
     * Get the orchestrator worker.
     */
    public AgentWorkerService getOrchestrator() {
        return workers.get("orchestrator");
    }

    /**
     * Route: use orchestrator to determine the right agent, then call it.
     */
    public String chat(String message) throws Exception {
        AgentWorkerService orchestrator = getOrchestrator();
        if (orchestrator == null || !orchestrator.isReady()) {
            return "Error: Orchestrator is not ready.";
        }
        return orchestrator.chat(message);
    }

    /**
     * Execute a multi-agent orchestration chain.
     * @param message  user input
     * @param roles    ordered list of roles to execute
     * @return chain results as JSON string
     */
    public String orchestrate(String message, List<String> roles) throws Exception {
        AgentWorkerService orchestrator = getOrchestrator();
        if (orchestrator == null || !orchestrator.isReady()) {
            return "Error: Orchestrator is not ready.";
        }
        Map<String, Object> req = new HashMap<>();
        req.put("message", message);
        req.put("action", "orchestrate");
        req.put("roles", roles);
        return orchestrator.send(mapper.writeValueAsString(req));
    }

    /**
     * Get all agents status info.
     */
    public Map<String, Object> getAllAgentInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, AgentWorkerService> entry : workers.entrySet()) {
            AgentWorkerService w = entry.getValue();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("role", w.getRole());
            info.put("ready", w.isReady());
            result.put(entry.getKey(), info);
        }
        return result;
    }

    /**
     * Check if a specific worker is ready.
     */
    public boolean isReady(String role) {
        AgentWorkerService w = workers.get(role);
        return w != null && w.isReady();
    }

    /**
     * Check if the orchestrator is ready.
     */
    public boolean isOrchestratorReady() {
        return isReady("orchestrator");
    }
}
