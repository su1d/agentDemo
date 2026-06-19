package com.agent.controller;

import com.agent.model.ChatRequest;
import com.agent.model.ChatResponse;
import com.agent.service.AgentWorkerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentWorkerManager manager;
    private final ObjectMapper mapper = new ObjectMapper();

    private String extractReplyContent(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) return rawReply;
        try {
            var node = mapper.readTree(rawReply);
            if (node.has("type") && "reply".equals(node.get("type").asText()) && node.has("content")) {
                var contentNode = node.get("content");
                if (contentNode.isTextual()) {
                    return contentNode.asText();
                } else {
                    return contentNode.toPrettyString();
                }
            }
        } catch (Exception ignored) {}
        return rawReply;
    }

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
            String rawReply;

            switch (action) {
                case "orchestrate": {
                    List<String> roles = request.getRoles();
                    if (roles == null) roles = List.of("calculator", "summarizer");
                    rawReply = manager.orchestrate(message, roles);
                    break;
                }
                case "parallel":
                    rawReply = manager.parallelCall(message, request.getRoles());
                    break;
                case "debate": {
                    int rounds = request.getRounds() != null ? request.getRounds() : 2;
                    rawReply = manager.debate(message, request.getRoles(), rounds);
                    break;
                }
                case "critique_chain": {
                    String genRole = request.getGenerateRole() != null ? request.getGenerateRole() : "searcher";
                    String critRole = request.getCritiqueRole() != null ? request.getCritiqueRole() : "summarizer";
                    int refine = request.getRefineRounds() != null ? request.getRefineRounds() : 2;
                    rawReply = manager.critiqueChain(message, genRole, critRole, refine);
                    break;
                }
                case "voting_consensus":
                    rawReply = manager.votingConsensus(message, request.getRoles());
                    break;
                case "brainstorm":
                    rawReply = manager.brainstorm(message, request.getRoles());
                    break;
                case "chat_all":
                    rawReply = manager.chatWithAll(message);
                    break;
                case "list_modes":
                    rawReply = manager.listModes();
                    break;
                default:
                    rawReply = manager.chat(message);
                    break;
            }
            String reply = extractReplyContent(rawReply);
            return new ChatResponse(reply, action, null);
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
