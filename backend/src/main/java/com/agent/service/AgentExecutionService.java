package com.agent.service;

import com.agent.entity.AgentExecutionLog;
import com.agent.entity.AgentWorkflow;
import com.agent.entity.AgentWorkflowNode;
import com.agent.repository.AgentExecutionLogRepository;
import com.agent.repository.AgentWorkflowNodeRepository;
import com.agent.repository.AgentWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 落库服务：记录 Agent 执行日志、管理工作流定义
 */
@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);

    private final AgentExecutionLogRepository executionLogRepository;
    private final AgentWorkflowRepository workflowRepository;
    private final AgentWorkflowNodeRepository workflowNodeRepository;

    public AgentExecutionService(AgentExecutionLogRepository executionLogRepository,
                                  AgentWorkflowRepository workflowRepository,
                                  AgentWorkflowNodeRepository workflowNodeRepository) {
        this.executionLogRepository = executionLogRepository;
        this.workflowRepository = workflowRepository;
        this.workflowNodeRepository = workflowNodeRepository;
    }

    // ========== Execution Log ==========

    /**
     * 创建执行日志记录
     */
    @Transactional
    public AgentExecutionLog createLog(String sessionId, String agentRole, String inputText,
                                        String collaborationMode, Long workflowId) {
        AgentExecutionLog logEntry = new AgentExecutionLog(sessionId, agentRole, inputText);
        logEntry.setCollaborationMode(collaborationMode);
        logEntry.setWorkflowId(workflowId);
        logEntry.setStartedAt(LocalDateTime.now());
        AgentExecutionLog saved = executionLogRepository.save(logEntry);
        log.info("Execution log created: session={}, role={}, mode={}", sessionId, agentRole, collaborationMode);
        return saved;
    }

    /**
     * 更新执行日志为成功
     */
    @Transactional
    public void completeLog(Long logId, String outputText, long durationMs) {
        executionLogRepository.findById(logId).ifPresent(logEntry -> {
            logEntry.setOutputText(outputText);
            logEntry.setStatus("success");
            logEntry.setDurationMs(durationMs);
            logEntry.setEndedAt(LocalDateTime.now());
            executionLogRepository.save(logEntry);
        });
    }

    /**
     * 更新执行日志为失败
     */
    @Transactional
    public void failLog(Long logId, String errorMessage, long durationMs) {
        executionLogRepository.findById(logId).ifPresent(logEntry -> {
            logEntry.setStatus("failed");
            logEntry.setErrorMessage(errorMessage);
            logEntry.setDurationMs(durationMs);
            logEntry.setEndedAt(LocalDateTime.now());
            executionLogRepository.save(logEntry);
        });
    }

    /**
     * 查询会话的所有执行记录
     */
    public List<AgentExecutionLog> getSessionLogs(String sessionId) {
        return executionLogRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    // ========== Workflow Management ==========

    /**
     * 创建工作流定义
     */
    @Transactional
    public AgentWorkflow createWorkflow(String name, String collaborationMode, String description) {
        AgentWorkflow wf = new AgentWorkflow(name, collaborationMode);
        wf.setDescription(description);
        AgentWorkflow saved = workflowRepository.save(wf);
        log.info("Workflow created: id={}, name={}, mode={}", saved.getId(), name, collaborationMode);
        return saved;
    }

    /**
     * 向工作流添加节点
     */
    @Transactional
    public AgentWorkflowNode addWorkflowNode(Long workflowId, String name, String agentRole,
                                              int sortOrder, int timeoutSeconds) {
        AgentWorkflowNode node = new AgentWorkflowNode();
        node.setWorkflowId(workflowId);
        node.setName(name);
        node.setAgentRole(agentRole);
        node.setSortOrder(sortOrder);
        node.setTimeoutSeconds(timeoutSeconds);
        node.setRetryCount(2);
        return workflowNodeRepository.save(node);
    }

    /**
     * 获取工作流的节点列表
     */
    public List<AgentWorkflowNode> getWorkflowNodes(Long workflowId) {
        return workflowNodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
    }

    /**
     * 生成唯一会话 ID
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
