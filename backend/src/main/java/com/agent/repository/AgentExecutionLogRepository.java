package com.agent.repository;

import com.agent.entity.AgentExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentExecutionLogRepository extends JpaRepository<AgentExecutionLog, Long> {
    List<AgentExecutionLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<AgentExecutionLog> findByWorkflowIdOrderByCreatedAtAsc(Long workflowId);
    List<AgentExecutionLog> findByStatus(String status);
}
