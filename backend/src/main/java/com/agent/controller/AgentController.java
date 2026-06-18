package com.agent.controller;

import com.agent.model.ChatRequest;
import com.agent.model.ChatResponse;
import com.agent.service.AgentWorkerManager;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentWorkerManager manager;

    public AgentController(AgentWorkerManager manager) {
        this.manager = manager;
    }

    /**
     * 聊天入口：自动路由到最合适的 Agent
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        try {
            String reply = manager.chat(request.getMessage());
            return new ChatResponse(reply);
        } catch (Exception e) {
            return new ChatResponse("错误: " + e.getMessage());
        }
    }

    /**
     * 编排模式：让多个 Agent 链式协作
     * @param request  { "message": "...", "roles": ["calculator", "summarizer"] }
     */
    @PostMapping("/chat/orchestrate")
    public ChatResponse orchestrate(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.getOrDefault("message", "");
            List<String> roles = (List<String>) request.getOrDefault("roles",
                    List.of("calculator", "weather", "summarizer"));
            String reply = manager.orchestrate(message, roles);
            return new ChatResponse(reply);
        } catch (Exception e) {
            return new ChatResponse("错误: " + e.getMessage());
        }
    }

    /**
     * 查看所有 Agent 状态
     */
    @GetMapping("/agents")
    public Map<String, Object> listAgents() {
        return manager.getAllAgentInfo();
    }

    /**
     * 查看系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agents", manager.getAllAgentInfo());
        result.put("orchestrator_ready", manager.isOrchestratorReady());
        result.put("status", manager.isOrchestratorReady() ? "正常" : "不可用");
        return result;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", manager.isOrchestratorReady() ? "正常" : "降级");
        long readyCount = manager.getAllAgentInfo().values().stream()
                .filter(m -> (boolean) ((Map) m).get("ready")).count();
        long total = manager.getAllAgentInfo().size();
        result.put("agents_ready", readyCount + "/" + total);
        return result;
    }
}
