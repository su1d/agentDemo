package com.agent.service;

import com.agent.entity.AgentWorkflow;
import com.agent.entity.AgentWorkflowEdge;
import com.agent.entity.AgentWorkflowNode;
import com.agent.entity.AgentExecutionLog;
import com.agent.repository.AgentWorkflowRepository;
import com.agent.repository.AgentWorkflowNodeRepository;
import com.agent.repository.AgentWorkflowEdgeRepository;
import com.agent.repository.AgentExecutionLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DAG-based workflow orchestration engine.
 * Supports: sequential, parallel (fan-out/fan-in), conditional branching.
 */
@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final AgentWorkflowRepository workflowRepository;
    private final AgentWorkflowNodeRepository nodeRepository;
    private final AgentWorkflowEdgeRepository edgeRepository;
    private final AgentExecutionLogRepository executionLogRepository;
    private final AgentWorkerManager workerManager;

    private final ExecutorService parallelExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public WorkflowExecutionService(AgentWorkflowRepository workflowRepository,
                                     AgentWorkflowNodeRepository nodeRepository,
                                     AgentWorkflowEdgeRepository edgeRepository,
                                     AgentExecutionLogRepository executionLogRepository,
                                     AgentWorkerManager workerManager) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.executionLogRepository = executionLogRepository;
        this.workerManager = workerManager;
    }

    // ========== Workflow CRUD ==========

    @Transactional
    public AgentWorkflow createWorkflow(String name, String description) {
        AgentWorkflow wf = new AgentWorkflow(name, "workflow");
        wf.setDescription(description);
        wf.setStatus("draft");
        return workflowRepository.save(wf);
    }

    @Transactional
    public AgentWorkflow updateWorkflow(Long id, String name, String description, String status) {
        AgentWorkflow wf = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        if (name != null) wf.setName(name);
        if (description != null) wf.setDescription(description);
        if (status != null) wf.setStatus(status);
        return workflowRepository.save(wf);
    }

    public List<AgentWorkflow> listWorkflows() {
        return workflowRepository.findAll();
    }

    public AgentWorkflow getWorkflow(Long id) {
        return workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        edgeRepository.deleteByWorkflowId(id);
        nodeRepository.deleteAll(nodeRepository.findByWorkflowIdOrderBySortOrderAsc(id));
        workflowRepository.deleteById(id);
    }

    // ========== Node CRUD ==========

    @Transactional
    public AgentWorkflowNode addNode(Long workflowId, String name, String nodeType, String agentRole,
                                      Double posX, Double posY, String inputTemplate, String outputMapping,
                                      String conditionExpression, String parallelRoles,
                                      Integer timeoutSeconds, Integer retryCount) {
        AgentWorkflowNode node = new AgentWorkflowNode();
        node.setWorkflowId(workflowId);
        node.setName(name);
        node.setNodeType(nodeType != null ? nodeType : "task");
        node.setAgentRole(agentRole);
        node.setPosX(posX);
        node.setPosY(posY);
        node.setInputTemplate(inputTemplate);
        node.setOutputMapping(outputMapping);
        node.setConditionExpression(conditionExpression);
        node.setParallelRoles(parallelRoles);
        node.setTimeoutSeconds(timeoutSeconds != null ? timeoutSeconds : 60);
        node.setRetryCount(retryCount != null ? retryCount : 0);

        // Determine sort_order
        List<AgentWorkflowNode> existing = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
        node.setSortOrder(existing.size());

        return nodeRepository.save(node);
    }

    @Transactional
    public AgentWorkflowNode updateNode(Long nodeId, String name, String nodeType, String agentRole,
                                         Double posX, Double posY,
                                         String conditionExpression, String parallelRoles) {
        AgentWorkflowNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("Node not found: " + nodeId));
        if (name != null) node.setName(name);
        if (nodeType != null) node.setNodeType(nodeType);
        if (agentRole != null) node.setAgentRole(agentRole);
        if (posX != null) node.setPosX(posX);
        if (posY != null) node.setPosY(posY);
        if (conditionExpression != null) node.setConditionExpression(conditionExpression);
        if (parallelRoles != null) node.setParallelRoles(parallelRoles);
        return nodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        edgeRepository.deleteAll(edgeRepository.findBySourceNodeId(nodeId));
        edgeRepository.deleteAll(edgeRepository.findByTargetNodeId(nodeId));
        nodeRepository.deleteById(nodeId);
    }

    public List<AgentWorkflowNode> getWorkflowNodes(Long workflowId) {
        return nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
    }

    // ========== Edge CRUD ==========

    @Transactional
    public AgentWorkflowEdge addEdge(Long workflowId, Long sourceNodeId, Long targetNodeId,
                                      String conditionExpression, String label) {
        AgentWorkflowEdge edge = new AgentWorkflowEdge(workflowId, sourceNodeId, targetNodeId);
        edge.setConditionExpression(conditionExpression);
        edge.setLabel(label);
        return edgeRepository.save(edge);
    }

    @Transactional
    public void deleteEdge(Long edgeId) {
        edgeRepository.deleteById(edgeId);
    }

    public List<AgentWorkflowEdge> getWorkflowEdges(Long workflowId) {
        return edgeRepository.findByWorkflowId(workflowId);
    }

    // ========== Full Workflow DTO for editor ==========

    public Map<String, Object> getWorkflowGraph(Long workflowId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflow", getWorkflow(workflowId));
        result.put("nodes", getWorkflowNodes(workflowId));
        result.put("edges", getWorkflowEdges(workflowId));
        return result;
    }

    // ========== DAG Execution Engine ==========

    /**
     * Execute a workflow by name with given input variables.
     * Returns execution results for each node.
     */
    public Map<String, Object> executeWorkflowByName(String workflowName, Map<String, Object> inputVariables) {
        List<AgentWorkflow> workflows = workflowRepository.findByStatus("active");
        AgentWorkflow wf = workflows.stream()
            .filter(w -> w.getName().equals(workflowName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Active workflow not found: " + workflowName));
        return executeWorkflow(wf.getId(), inputVariables);
    }

    /**
     * Execute a workflow by ID with given input variables.
     */
    public Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> inputVariables) {
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> context = new HashMap<>(inputVariables);
        if (!context.containsKey("sessionId")) {
            context.put("sessionId", sessionId);
        }

        List<AgentWorkflowNode> allNodes = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
        List<AgentWorkflowEdge> allEdges = edgeRepository.findByWorkflowId(workflowId);

        Map<Long, AgentWorkflowNode> nodeMap = allNodes.stream()
            .collect(Collectors.toMap(AgentWorkflowNode::getId, n -> n));

        // Build adjacency list
        Map<Long, List<AgentWorkflowEdge>> outEdges = new HashMap<>();
        Map<Long, List<AgentWorkflowEdge>> inEdges = new HashMap<>();
        for (AgentWorkflowEdge edge : allEdges) {
            outEdges.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            inEdges.computeIfAbsent(edge.getTargetNodeId(), k -> new ArrayList<>()).add(edge);
        }

        // Find start nodes (no incoming edges or node_type=start)
        List<AgentWorkflowNode> startNodes = allNodes.stream()
            .filter(n -> "start".equals(n.getNodeType()) || !inEdges.containsKey(n.getId()))
            .collect(Collectors.toList());

        if (startNodes.isEmpty() && !allNodes.isEmpty()) {
            startNodes.add(allNodes.get(0));
        }

        // Topological order for sequential portions
        List<Long> topoOrder = topologicalSort(allNodes, outEdges);

        Map<Long, Map<String, Object>> nodeResults = new LinkedHashMap<>();
        Set<Long> completed = new HashSet<>();
        Set<Long> failed = new HashSet<>();

        // Execute start nodes
        for (AgentWorkflowNode startNode : startNodes) {
            executeNode(startNode, context, nodeMap, outEdges, inEdges,
                       nodeResults, completed, failed, sessionId, workflowId, topoOrder);
        }

        // Build final output
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("workflowId", workflowId);
        result.put("status", failed.isEmpty() ? "success" : "completed_with_errors");
        result.put("context", context);

        List<Map<String, Object>> nodeResultList = new ArrayList<>();
        for (AgentWorkflowNode node : topoOrder.stream().map(nodeMap::get).filter(Objects::nonNull).collect(Collectors.toList())) {
            Map<String, Object> nr = nodeResults.get(node.getId());
            if (nr != null) {
                nodeResultList.add(nr);
            }
        }
        result.put("nodeResults", nodeResultList);

        return result;
    }

    private void executeNode(AgentWorkflowNode node, Map<String, Object> context,
                              Map<Long, AgentWorkflowNode> nodeMap,
                              Map<Long, List<AgentWorkflowEdge>> outEdges,
                              Map<Long, List<AgentWorkflowEdge>> inEdges,
                              Map<Long, Map<String, Object>> nodeResults,
                              Set<Long> completed, Set<Long> failed,
                              String sessionId, Long workflowId,
                              List<Long> topoOrder) {
        if (completed.contains(node.getId()) || failed.contains(node.getId())) return;

        // Check all predecessors completed
        List<AgentWorkflowEdge> incoming = inEdges.getOrDefault(node.getId(), List.of());
        for (AgentWorkflowEdge edge : incoming) {
            Long predId = edge.getSourceNodeId();
            if (!completed.contains(predId) && !"start".equals(nodeMap.get(predId).getNodeType())) {
                return; // Not ready yet
            }
            // Check condition
            if (edge.getConditionExpression() != null && !edge.getConditionExpression().isEmpty()) {
                Object parentResult = nodeResults.get(predId);
                if (!evaluateCondition(edge.getConditionExpression(), parentResult, context)) {
                    return; // Condition not met
                }
            }
        }

        log.info("Executing node: {} (type={}, role={})", node.getName(), node.getNodeType(), node.getAgentRole());

        Map<String, Object> nodeResult = new LinkedHashMap<>();
        nodeResult.put("nodeId", node.getId());
        nodeResult.put("nodeName", node.getName());
        nodeResult.put("nodeType", node.getNodeType());
        nodeResult.put("startedAt", LocalDateTime.now().toString());

        try {
            switch (node.getNodeType()) {
                case "start":
                    nodeResult.put("status", "success");
                    nodeResult.put("output", "started");
                    break;

                case "end":
                    nodeResult.put("status", "success");
                    nodeResult.put("output", context.getOrDefault("finalResult", "completed"));
                    break;

                case "task":
                    String taskResult = executeTaskNode(node, context, sessionId, workflowId);
                    nodeResult.put("status", "success");
                    nodeResult.put("output", taskResult);
                    // Store output in context using node name as key
                    context.put(node.getName() + "_output", taskResult);
                    break;

                case "parallel":
                    Map<String, Object> parallelResult = executeParallelNode(node, context, sessionId, workflowId);
                    nodeResult.put("status", "success");
                    nodeResult.put("output", parallelResult);
                    nodeResult.put("parallelResults", parallelResult);
                    context.put(node.getName() + "_output", parallelResult);
                    break;

                case "condition":
                    // Condition node just passes through; edges handle routing
                    nodeResult.put("status", "success");
                    nodeResult.put("output", "evaluated");
                    break;

                case "merge":
                    // Collect outputs from all predecessors
                    Map<String, Object> mergeData = new LinkedHashMap<>();
                    for (AgentWorkflowEdge inEdge : incoming) {
                        Long sourceId = inEdge.getSourceNodeId();
                        AgentWorkflowNode sourceNode = nodeMap.get(sourceId);
                        if (sourceNode != null && nodeResults.containsKey(sourceId)) {
                            mergeData.put(sourceNode.getName() + "_output",
                                nodeResults.get(sourceId).get("output"));
                        }
                    }
                    nodeResult.put("status", "success");
                    nodeResult.put("output", mergeData);
                    context.put(node.getName() + "_output", mergeData);
                    break;

                default:
                    nodeResult.put("status", "failed");
                    nodeResult.put("error", "Unknown node type: " + node.getNodeType());
                    failed.add(node.getId());
                    return;
            }

            nodeResult.put("endedAt", LocalDateTime.now().toString());
            nodeResults.put(node.getId(), nodeResult);
            completed.add(node.getId());

            // Execute downstream nodes
            List<AgentWorkflowEdge> outgoing = outEdges.getOrDefault(node.getId(), List.of());
            for (AgentWorkflowEdge edge : outgoing) {
                Long targetId = edge.getTargetNodeId();
                AgentWorkflowNode targetNode = nodeMap.get(targetId);
                if (targetNode != null) {
                    executeNode(targetNode, context, nodeMap, outEdges, inEdges,
                               nodeResults, completed, failed, sessionId, workflowId, topoOrder);
                }
            }

        } catch (Exception e) {
            log.error("Node execution failed: {}", node.getName(), e);
            nodeResult.put("status", "failed");
            nodeResult.put("error", e.getMessage());
            nodeResult.put("endedAt", LocalDateTime.now().toString());
            nodeResults.put(node.getId(), nodeResult);
            failed.add(node.getId());
        }
    }

    private String executeTaskNode(AgentWorkflowNode node, Map<String, Object> context,
                                    String sessionId, Long workflowId) throws Exception {
        String agentRole = node.getAgentRole();
        if (agentRole == null || agentRole.isEmpty()) {
            return "No agent role configured for task node";
        }

        AgentWorkerService worker = workerManager.getWorker(agentRole);
        if (worker == null || !worker.isReady()) {
            return "Agent [" + agentRole + "] is not available";
        }

        // Build prompt from input template with variable substitution
        String prompt = node.getInputTemplate();
        if (prompt == null || prompt.isEmpty()) {
            prompt = (String) context.getOrDefault("message", "");
        } else {
            prompt = substituteVariables(prompt, context);
        }

        String req = mapper.writeValueAsString(Map.of("message", prompt));
        String response = worker.sendWithLog(req, sessionId, "workflow");

        // Parse response
        var json = mapper.readTree(response);
        String content = json.get("content").asText();

        // Apply output mapping if configured
        String outputMapping = node.getOutputMapping();
        if (outputMapping != null && !outputMapping.isEmpty()) {
            Map<String, String> mapping = mapper.readValue(outputMapping,
                new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                context.put(entry.getKey(), content);
            }
        }

        return content;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeParallelNode(AgentWorkflowNode node, Map<String, Object> context,
                                                     String sessionId, Long workflowId) throws Exception {
        String rolesStr = node.getParallelRoles();
        if (rolesStr == null || rolesStr.isEmpty()) {
            rolesStr = "calculator,searcher,summarizer";
        }
        String[] roles = rolesStr.split(",");

        String prompt = node.getInputTemplate();
        if (prompt == null || prompt.isEmpty()) {
            prompt = (String) context.getOrDefault("message", "");
        } else {
            prompt = substituteVariables(prompt, context);
        }

        final Map<String, Object> results = new ConcurrentHashMap<>();
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        Map<String, CompletableFuture<Void>> futureMap = new HashMap<>();
        for (String r : roles) {
            final String role = r.trim();
            AgentWorkerService worker = workerManager.getWorker(role);
            if (worker == null || !worker.isReady()) {
                results.put(role, "Agent [" + role + "] not available");
                continue;
            }

            final AgentWorkerService finalWorker = worker;
            final String finalPrompt = prompt;
            final String finalSessionId = sessionId;
            final String finalRole = role;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String req = mapper.writeValueAsString(Map.of("message", finalPrompt));
                    String resp = finalWorker.sendWithLog(req, finalSessionId, "workflow_parallel");
                    var json = mapper.readTree(resp);
                    results.put(finalRole, json.get("content").asText());
                } catch (Exception e) {
                    results.put(finalRole, "Error: " + e.getMessage());
                }
            }, parallelExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "parallel");
        result.put("individual_results", results);

        // Auto-summarize parallel results
        StringBuilder summary = new StringBuilder("楠炴儼顢戞径鍕倞缂佹挻鐏?\n");
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            summary.append("[").append(entry.getKey()).append("]: ").append(entry.getValue()).append("\n");
        }
        result.put("summary", summary.toString());

        return result;
    }

    private boolean evaluateCondition(String expression, Object parentResult, Map<String, Object> context) {
        if (expression == null || expression.isEmpty()) return true;
        try {
            // Simple expression evaluation
            // Supports: "success", "contains:xxx", "equals:xxx:yyy"
            if (expression.equals("success")) {
                if (parentResult instanceof Map) {
                    String status = (String) ((Map<String, Object>) parentResult).get("status");
                    return "success".equals(status);
                }
                return true;
            }
            if (expression.startsWith("contains:")) {
                String keyword = expression.substring(9);
                if (parentResult instanceof Map) {
                    Object output = ((Map<String, Object>) parentResult).get("output");
                    return output != null && output.toString().contains(keyword);
                }
                return false;
            }
            if (expression.startsWith("equals:")) {
                String[] parts = expression.substring(7).split(":", 2);
                if (parts.length == 2) {
                    Object value = context.get(parts[0]);
                    return parts[1].equals(String.valueOf(value));
                }
                return false;
            }
            // Default: true
            return true;
        } catch (Exception e) {
            log.warn("Condition evaluation failed: {}", expression, e);
            return false;
        }
    }

    private String substituteVariables(String template, Map<String, Object> context) {
        if (template == null) return null;
        Pattern pattern = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.getOrDefault(varName, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<Long> topologicalSort(List<AgentWorkflowNode> nodes,
                                        Map<Long, List<AgentWorkflowEdge>> outEdges) {
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, AgentWorkflowNode> nodeMap = nodes.stream()
            .collect(Collectors.toMap(AgentWorkflowNode::getId, n -> n));

        for (AgentWorkflowNode node : nodes) {
            inDegree.putIfAbsent(node.getId(), 0);
        }
        for (List<AgentWorkflowEdge> edges : outEdges.values()) {
            for (AgentWorkflowEdge edge : edges) {
                inDegree.merge(edge.getTargetNodeId(), 1, Integer::sum);
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Long> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long nodeId = queue.poll();
            order.add(nodeId);
            for (AgentWorkflowEdge edge : outEdges.getOrDefault(nodeId, List.of())) {
                Long targetId = edge.getTargetNodeId();
                inDegree.merge(targetId, -1, Integer::sum);
                if (inDegree.get(targetId) == 0) {
                    queue.add(targetId);
                }
            }
        }

        return order;
    }
}
