package com.agent.repository;

import com.agent.entity.AgentWorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentWorkflowNodeRepository extends JpaRepository<AgentWorkflowNode, Long> {
    List<AgentWorkflowNode> findByWorkflowIdOrderBySortOrderAsc(Long workflowId);
}
