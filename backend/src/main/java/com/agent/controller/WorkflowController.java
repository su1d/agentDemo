package com.agent.controller;

import com.agent.entity.AgentWorkflow;
import com.agent.entity.AgentWorkflowEdge;
import com.agent.entity.AgentWorkflowNode;
import com.agent.service.WorkflowExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowExecutionService workflowService;

    public WorkflowController(WorkflowExecutionService workflowService) {
        this.workflowService = workflowService;
    }

    // ========== Workflow CRUD ==========

    @PostMapping
    public ResponseEntity<?> createWorkflow(@RequestBody Map<String, String> body) {
        try {
            AgentWorkflow wf = workflowService.createWorkflow(
                body.get("name"), body.get("description"));
            return ResponseEntity.ok(wf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listWorkflows() {
        return ResponseEntity.ok(workflowService.listWorkflows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflow(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(workflowService.getWorkflowGraph(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkflow(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            AgentWorkflow wf = workflowService.updateWorkflow(
                id, body.get("name"), body.get("description"), body.get("status"));
            return ResponseEntity.ok(wf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable Long id) {
        try {
            workflowService.deleteWorkflow(id);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Node CRUD ==========

    @PostMapping("/{workflowId}/nodes")
    public ResponseEntity<?> addNode(@PathVariable Long workflowId, @RequestBody Map<String, Object> body) {
        try {
            AgentWorkflowNode node = workflowService.addNode(
                workflowId,
                (String) body.get("name"),
                (String) body.get("nodeType"),
                (String) body.get("agentRole"),
                body.get("posX") != null ? ((Number) body.get("posX")).doubleValue() : null,
                body.get("posY") != null ? ((Number) body.get("posY")).doubleValue() : null,
                (String) body.get("inputTemplate"),
                (String) body.get("outputMapping"),
                (String) body.get("conditionExpression"),
                (String) body.get("parallelRoles"),
                body.get("timeoutSeconds") != null ? ((Number) body.get("timeoutSeconds")).intValue() : null,
                body.get("retryCount") != null ? ((Number) body.get("retryCount")).intValue() : null
            );
            return ResponseEntity.ok(node);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/nodes/{nodeId}")
    public ResponseEntity<?> updateNode(@PathVariable Long nodeId, @RequestBody Map<String, Object> body) {
        try {
            AgentWorkflowNode node = workflowService.updateNode(
                nodeId,
                (String) body.get("name"),
                (String) body.get("nodeType"),
                (String) body.get("agentRole"),
                body.get("posX") != null ? ((Number) body.get("posX")).doubleValue() : null,
                body.get("posY") != null ? ((Number) body.get("posY")).doubleValue() : null,
                (String) body.get("conditionExpression"),
                (String) body.get("parallelRoles")
            );
            return ResponseEntity.ok(node);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<?> deleteNode(@PathVariable Long nodeId) {
        try {
            workflowService.deleteNode(nodeId);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Edge CRUD ==========

    @PostMapping("/{workflowId}/edges")
    public ResponseEntity<?> addEdge(@PathVariable Long workflowId, @RequestBody Map<String, Object> body) {
        try {
            AgentWorkflowEdge edge = workflowService.addEdge(
                workflowId,
                ((Number) body.get("sourceNodeId")).longValue(),
                ((Number) body.get("targetNodeId")).longValue(),
                (String) body.get("conditionExpression"),
                (String) body.get("label")
            );
            return ResponseEntity.ok(edge);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/edges/{edgeId}")
    public ResponseEntity<?> deleteEdge(@PathVariable Long edgeId) {
        try {
            workflowService.deleteEdge(edgeId);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Execution ==========

    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeWorkflow(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables", Map.of());
            Map<String, Object> result = workflowService.executeWorkflow(id, variables);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/execute-by-name")
    public ResponseEntity<?> executeByName(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables", Map.of());
            Map<String, Object> result = workflowService.executeWorkflowByName(name, variables);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
