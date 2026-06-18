package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Edge connecting two nodes in a workflow DAG.
 * Supports conditional branching via conditionExpression.
 */
@Entity
@Table(name = "agent_workflow_edge")
public class AgentWorkflowEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    /** Optional condition expression (SpEL or simple expression) for conditional edges */
    @Column(name = "condition_expression", length = 500)
    private String conditionExpression;

    /** Label displayed on the edge (e.g. "yes", "no", ">5") */
    @Column(length = 100)
    private String label;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public AgentWorkflowEdge() {}

    public AgentWorkflowEdge(Long workflowId, Long sourceNodeId, Long targetNodeId) {
        this.workflowId = workflowId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }

    public Long getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(Long sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public Long getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(Long targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
