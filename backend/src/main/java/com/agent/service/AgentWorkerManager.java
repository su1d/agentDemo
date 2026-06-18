package com.agent.service;

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
 * Supports multiple collaboration modes with DB logging.
 */
@Service
public class AgentWorkerManager {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkerManager.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, AgentWorkerService> workers = new ConcurrentHashMap<>();

    // Injected execution service for DB logging
    private AgentExecutionService executionService;

    private static final List<String> ROLES = List.of(
        "orchestrator", "calculator", "weather", "searcher", "summarizer"
    );

    public AgentWorkerManager(AgentExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostConstruct
    public void init() {
        for (String role : ROLES) {
            AgentWorkerService worker = new AgentWorkerService(role);
            worker.setExecutionService(executionService);
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

    public AgentWorkerService getWorker(String role) {
        return workers.get(role);
    }

    public AgentWorkerService getOrchestrator() {
        return workers.get("orchestrator");
    }

    public String chat(String message) throws Exception {
        AgentWorkerService orchestrator = getOrchestrator();
        if (orchestrator == null || !orchestrator.isReady()) {
            return "Error: Orchestrator is not ready.";
        }
        String sessionId = executionService.generateSessionId();
        String req = mapper.writeValueAsString(Map.of("message", message));
        return orchestrator.sendWithLog(req, sessionId, "auto");
    }

    public String orchestrate(String message, List<String> roles) throws Exception {
        return sendToOrchestrator("orchestrate", message, Map.of("roles", roles));
    }

    public String parallelCall(String message, List<String> roles) throws Exception {
        Map<String, Object> params = new HashMap<>();
        if (roles != null) params.put("roles", roles);
        return sendToOrchestrator("parallel", message, params);
    }

    public String debate(String message, List<String> roles, int rounds) throws Exception {
        Map<String, Object> params = new HashMap<>();
        if (roles != null) params.put("roles", roles);
        params.put("rounds", rounds);
        return sendToOrchestrator("debate", message, params);
    }

    public String critiqueChain(String message, String generateRole, String critiqueRole, int refineRounds) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("generate_role", generateRole);
        params.put("critique_role", critiqueRole);
        params.put("refine_rounds", refineRounds);
        return sendToOrchestrator("critique_chain", message, params);
    }

    public String votingConsensus(String message, List<String> roles) throws Exception {
        Map<String, Object> params = new HashMap<>();
        if (roles != null) params.put("roles", roles);
        return sendToOrchestrator("voting_consensus", message, params);
    }

    public String brainstorm(String message, List<String> roles) throws Exception {
        Map<String, Object> params = new HashMap<>();
        if (roles != null) params.put("roles", roles);
        return sendToOrchestrator("brainstorm", message, params);
    }

    public String chatWithAll(String message) throws Exception {
        return sendToOrchestrator("chat_all", message, Map.of());
    }

    public String listModes() throws Exception {
        return sendToOrchestrator("list_modes", "", Map.of());
    }

    private String sendToOrchestrator(String action, String message, Map<String, Object> extraParams) throws Exception {
        AgentWorkerService orchestrator = getOrchestrator();
        if (orchestrator == null || !orchestrator.isReady()) {
            return "{\"type\":\"error\",\"content\":\"Orchestrator is not ready.\"}";
        }
        Map<String, Object> req = new LinkedHashMap<>(extraParams);
        req.put("message", message);
        req.put("action", action);

        String sessionId = executionService.generateSessionId();

        return orchestrator.sendWithLog(mapper.writeValueAsString(req), sessionId, action);
    }

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

    public boolean isReady(String role) {
        AgentWorkerService w = workers.get(role);
        return w != null && w.isReady();
    }

    public boolean isOrchestratorReady() {
        return isReady("orchestrator");
    }
}
