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
     * 统一协作入口：根据 action 参数路由到不同的协作模式
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        try {
            String message = request.getMessage();
            String action = request.getAction() != null ? request.getAction() : "chat";

            return switch (action) {
                case "orchestrate" -> {
                    List<String> roles = request.getRoles();
                    if (roles == null) roles = List.of("calculator", "summarizer");
                    String reply = manager.orchestrate(message, roles);
                    yield new ChatResponse(reply, "orchestrate", null);
                }
                case "parallel" -> {
                    List<String> roles = request.getRoles();
                    String reply = manager.parallelCall(message, roles);
                    yield new ChatResponse(reply, "parallel", null);
                }
                case "debate" -> {
                    List<String> roles = request.getRoles();
                    int rounds = request.getRounds() != null ? request.getRounds() : 2;
                    String reply = manager.debate(message, roles, rounds);
                    yield new ChatResponse(reply, "debate", null);
                }
                case "critique_chain" -> {
                    String genRole = request.getGenerateRole() != null ? request.getGenerateRole() : "searcher";
                    String critRole = request.getCritiqueRole() != null ? request.getCritiqueRole() : "summarizer";
                    int refine = request.getRefineRounds() != null ? request.getRefineRounds() : 2;
                    String reply = manager.critiqueChain(message, genRole, critRole, refine);
                    yield new ChatResponse(reply, "critique_chain", null);
                }
                case "voting_consensus" -> {
                    List<String> roles = request.getRoles();
                    String reply = manager.votingConsensus(message, roles);
                    yield new ChatResponse(reply, "voting_consensus", null);
                }
                case "brainstorm" -> {
                    List<String> roles = request.getRoles();
                    String reply = manager.brainstorm(message, roles);
                    yield new ChatResponse(reply, "brainstorm", null);
                }
                case "chat_all" -> {
                    String reply = manager.chatWithAll(message);
                    yield new ChatResponse(reply, "chat_all", null);
                }
                case "list_modes" -> {
                    String reply = manager.listModes();
                    yield new ChatResponse(reply, "list_modes", null);
                }
                default -> {
                    String reply = manager.chat(message);
                    yield new ChatResponse(reply, "auto", null);
                }
            };
        } catch (Exception e) {
            return new ChatResponse("错误: " + e.getMessage());
        }
    }

    /**
     * 编排模式（兼容旧端点）
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
        result.put("available_modes", List.of(
            "auto", "orchestrate", "parallel", "debate",
            "critique_chain", "voting_consensus", "brainstorm", "chat_all"
        ));
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
