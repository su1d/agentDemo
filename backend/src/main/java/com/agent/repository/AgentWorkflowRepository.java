package com.agent.repository;

import com.agent.entity.AgentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentWorkflowRepository extends JpaRepository<AgentWorkflow, Long> {
    List<AgentWorkflow> findByStatus(String status);
    List<AgentWorkflow> findByCollaborationMode(String collaborationMode);
}
