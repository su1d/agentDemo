package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Node in a workflow DAG. Supports multiple node types:
 * - task: Execute an agent role
 * - condition: Conditional branch (if/else)
 * - parallel: Fan-out to multiple parallel branches
 * - merge: Join parallel branches back
 * - start: Workflow start point
 * - end: Workflow end point
 */
@Entity
@Table(name = "agent_workflow_node")
public class AgentWorkflowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** Node type: start, end, task, condition, parallel, merge */
    @Column(name = "node_type", nullable = false, length = 20)
    private String nodeType = "task";

    /** Agent role for task nodes */
    @Column(name = "agent_role", length = 50)
    private String agentRole;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /** Position X in visual editor */
    @Column(name = "pos_x")
    private Double posX;

    /** Position Y in visual editor */
    @Column(name = "pos_y")
    private Double posY;

    /** Input template with variable substitution {{var}} */
    @Column(name = "input_template", columnDefinition = "TEXT")
    private String inputTemplate;

    /** Output mapping JSON: { "result_var": "$.content" } */
    @Column(name = "output_mapping", columnDefinition = "TEXT")
    private String outputMapping;

    /** Condition expression for condition nodes */
    @Column(name = "condition_expression", length = 500)
    private String conditionExpression;

    /** For parallel nodes: list of roles comma-separated */
    @Column(name = "parallel_roles", length = 500)
    private String parallelRoles;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 60;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public AgentWorkflowNode() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getAgentRole() { return agentRole; }
    public void setAgentRole(String agentRole) { this.agentRole = agentRole; }

    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Double getPosX() { return posX; }
    public void setPosX(Double posX) { this.posX = posX; }

    public Double getPosY() { return posY; }
    public void setPosY(Double posY) { this.posY = posY; }

    public String getInputTemplate() { return inputTemplate; }
    public void setInputTemplate(String inputTemplate) { this.inputTemplate = inputTemplate; }

    public String getOutputMapping() { return outputMapping; }
    public void setOutputMapping(String outputMapping) { this.outputMapping = outputMapping; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }

    public String getParallelRoles() { return parallelRoles; }
    public void setParallelRoles(String parallelRoles) { this.parallelRoles = parallelRoles; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
