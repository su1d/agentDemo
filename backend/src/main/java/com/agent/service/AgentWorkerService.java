package com.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.*;

/**
 * Manages a single Python LangGraph agent worker process.
 * Each worker has a role (orchestrator, calculator, weather, etc.).
 */
@Service
public class AgentWorkerService {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkerService.class);
    private static final String PYTHON = "python";
    private static final String WORKER_SCRIPT = "work/agent_worker.py";
    private static final File WORK_DIR = new File(System.getProperty("user.dir")).getAbsoluteFile();
    private static final long TIMEOUT_MS = 60000;

    private final String role;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;
    private volatile boolean ready = false;

    public AgentWorkerService() {
        this.role = "orchestrator";
    }

    public AgentWorkerService(String role) {
        this.role = role;
    }

    @PostConstruct
    public void start() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON, "-u", WORKER_SCRIPT);
            pb.directory(WORK_DIR);
            pb.environment().put("WORKER_ROLE", role);
            pb.redirectErrorStream(false);

            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

            // Read ready signal
            String readyLine = reader.readLine();
            if (readyLine != null) {
                JsonNode json = mapper.readTree(readyLine);
                if ("ready".equals(json.get("type").asText())) {
                    log.info("Agent worker [{}] ready, model: {}", role, json.get("model").asText());
                    ready = true;
                }
            }

            // Monitor stderr in background
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
     * Send a JSON command to the worker and get the response.
     */
    public String send(String jsonRequest) throws Exception {
        if (!ready || !running) {
            return "{\"type\":\"error\",\"content\":\"Agent [" + role + "] is not ready.\"}";
        }

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                synchronized (this) {
                    writer.write(jsonRequest + "\n");
                    writer.flush();
                    String responseLine = reader.readLine();
                    if (responseLine == null) {
                        return "{\"type\":\"error\",\"content\":\"Agent process disconnected.\"}";
                    }
                    log.info("Raw response from [{}]: {}", role, responseLine);
                    return responseLine;
                }
            } catch (Exception e) {
                return "{\"type\":\"error\",\"content\":\"" + e.getMessage() + "\"}";
            }
        }, executor);

        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return "{\"type\":\"error\",\"content\":\"Agent [" + role + "] timed out (> 60s).\"}";
        }
    }

    /**
     * Convenience: send a chat message, return the parsed reply content.
     */
    public String chat(String message) throws Exception {
        String req = mapper.writeValueAsString(java.util.Map.of("message", message));
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
}
