package com.agent.repository;

import com.agent.entity.AgentWorkflowEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentWorkflowEdgeRepository extends JpaRepository<AgentWorkflowEdge, Long> {
    List<AgentWorkflowEdge> findByWorkflowId(Long workflowId);
    List<AgentWorkflowEdge> findBySourceNodeId(Long sourceNodeId);
    List<AgentWorkflowEdge> findByTargetNodeId(Long targetNodeId);
    void deleteByWorkflowId(Long workflowId);
}
