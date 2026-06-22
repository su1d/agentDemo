package com.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;



/**
 * Manages a single Python LangGraph agent worker process.
 * Each worker has a role (orchestrator, calculator, weather, etc.).
 * Supports retry on timeout, sends execution logs to database.
 */
@Service
public class AgentWorkerService {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkerService.class);
    private static final String PYTHON = "python";
    private static final String WORKER_SCRIPT = "work/agent_worker.py";
    private static final File WORK_DIR = new File(System.getProperty("user.dir")).getAbsoluteFile();
    private static final long TIMEOUT_MS = 120000;
    private static final int MAX_RETRIES = 2;

    private final String role;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;
    private volatile boolean ready = false;

    private long totalCalls = 0;
    private long failedCalls = 0;
    private long totalDurationMs = 0;

    // Injected lazily to avoid circular dependency with AgentExecutionService
    private AgentExecutionService executionService;

    private ProviderConfigService providerConfigService;

    public AgentWorkerService() {
        this.role = "orchestrator";
    }

    public AgentWorkerService(String role) {
        this.role = role;
    }

    /** Setter for execution service (called by Manager after construction) */
    public void setExecutionService(AgentExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Setter for ProviderConfig (called by Manager after construction) */
    public void setProviderConfig(ProviderConfigService providerConfigService) {
        this.providerConfigService = providerConfigService;
    }

    @PostConstruct
    public void start() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON, "-u", WORKER_SCRIPT);
            pb.directory(WORK_DIR);
            pb.environment().put("WORKER_ROLE", role);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            String providerKey = providerConfigService != null ? providerConfigService.getPrimaryProviderKey() : "openai";
            String apiKey = providerConfigService != null ? providerConfigService.getApiKey(providerKey) : System.getenv("OPENAI_API_KEY");
            String baseUrl = providerConfigService != null ? providerConfigService.getBaseUrl(providerKey) : "https://api.openai.com/v1";
            String model = providerConfigService != null ? providerConfigService.getDefaultModel(providerKey) : "gpt-4o-mini";
            log.info("Starting agent worker [{}] with provider '{}', model: {}", role, providerKey, model);
            pb.environment().put("OPENAI_API_KEY", apiKey);
            pb.environment().put("OPENAI_BASE_URL", baseUrl);
            pb.environment().put("OPENAI_MODEL", model);
            pb.redirectErrorStream(false);

            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

            String readyLine = reader.readLine();
            if (readyLine != null) {
                JsonNode json = mapper.readTree(readyLine);
                if ("ready".equals(json.get("type").asText())) {
                    log.info("Agent worker [{}] ready, model: {}", role, json.get("model").asText());
                    ready = true;
                }
            }

            Thread errThread = new Thread(() -> {
                try (BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        log.warn("[Agent Worker {}] {}", role, line);
                    }
                } catch (IOException ignored) {}
            });
            errThread.setDaemon(true);
            errThread.start();

            running = true;
        } catch (Exception e) {
            log.error("Failed to start agent worker [{}]", role, e);
            ready = false;
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (process != null) {
            process.destroyForcibly();
        }
        executor.shutdownNow();
    }

    /**
     * Send a JSON command to the worker and get the response, with retry and DB logging.
     */
    public String send(String jsonRequest) throws Exception {
        return sendWithLog(jsonRequest, null, null);
    }

    /**
     * Send with execution logging.
     * @param jsonRequest JSON command
     * @param sessionId  session ID for logging, null to skip DB logging
     * @param collaborationMode collaboration mode name
     */
    public String sendWithLog(String jsonRequest, String sessionId, String collaborationMode) throws Exception {
        if (!ready || !running) {
            return "{\"type\":\"error\",\"content\":\"Agent [" + role + "] is not ready.\"}";
        }

        // Create DB log entry
        Long logId = null;
        if (executionService != null && sessionId != null) {
            try {
                var logEntry = executionService.createLog(sessionId, role, jsonRequest, collaborationMode, null);
                logId = logEntry.getId();
            } catch (Exception e) {
                log.warn("Failed to create execution log: {}", e.getMessage());
            }
        }

        long start = System.currentTimeMillis();
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= MAX_RETRIES) {
            attempt++;
            try {
                String result = sendOnce(jsonRequest);

                // Update DB log on success
                long duration = System.currentTimeMillis() - start;
                if (logId != null && executionService != null) {
                    try {
                        executionService.completeLog(logId, result, duration);
                    } catch (Exception e) {
                        log.warn("Failed to update execution log: {}", e.getMessage());
                    }
                }
                return result;

            } catch (TimeoutException e) {
                lastException = e;
                log.warn("Agent [{}] attempt {}/{} timed out", role, attempt, MAX_RETRIES);
                if (attempt <= MAX_RETRIES) {
                    log.info("Restarting agent worker [{}] for retry...", role);
                    stop();
                    start();
                }
            } catch (Exception e) {
                lastException = e;
                log.error("Agent [{}] attempt {}/{} failed: {}", role, attempt, MAX_RETRIES, e.getMessage());
                break;
            }
        }

        long duration = System.currentTimeMillis() - start;
        failedCalls++;

        // Update DB log on failure
        if (logId != null && executionService != null) {
            try {
                executionService.failLog(logId,
                    lastException != null ? lastException.getMessage() : "unknown", duration);
            } catch (Exception e) {
                log.warn("Failed to update execution log (fail): {}", e.getMessage());
            }
        }

        return "{\"type\":\"error\",\"content\":\"Agent [" + role + "] failed after " + attempt
            + " attempt(s): " + (lastException != null ? lastException.getMessage() : "unknown") + "\"}";
    }

    private String sendOnce(String jsonRequest) throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                synchronized (this) {
                    writer.write(jsonRequest + "\n");
                    writer.flush();
                    String responseLine = reader.readLine();
                    if (responseLine == null) {
                        throw new RuntimeException("Agent process disconnected.");
                    }
                    return responseLine;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    public String chat(String message) throws Exception {
        String req = mapper.writeValueAsString(Map.of("message", message));
        String resp = send(req);
        JsonNode json = mapper.readTree(resp);
        String type = json.get("type").asText();
        return switch (type) {
            case "reply" -> json.get("content").asText();
            case "error" -> "Error: " + json.get("content").asText();
            default -> "Error: Unknown response type: " + type;
        };
    }

    public boolean isReady() {
        return ready && running;
    }

    public String getRole() {
        return role;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("role", role);
        stats.put("ready", ready);
        stats.put("total_calls", totalCalls);
        stats.put("failed_calls", failedCalls);
        stats.put("avg_duration_ms", totalCalls > 0 ? totalDurationMs / totalCalls : 0);
        stats.put("total_duration_ms", totalDurationMs);
        return stats;
    }
}

