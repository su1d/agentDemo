-- Agent Platform Database Initialization

CREATE TABLE IF NOT EXISTS agent_workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '节点名称',
    description VARCHAR(500) COMMENT '节点描述',
    agent_role VARCHAR(50) NOT NULL COMMENT '关联Agent角色',
    workflow_id BIGINT NOT NULL COMMENT '所属工作流ID',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    input_template TEXT COMMENT '输入模板',
    output_mapping TEXT COMMENT '输出映射配置(JSON)',
    timeout_seconds INT DEFAULT 60 COMMENT '超时时间',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id)
);

CREATE TABLE IF NOT EXISTS agent_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '工作流名称',
    description VARCHAR(500) COMMENT '工作流描述',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft/active/disabled',
    created_by VARCHAR(100) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT COMMENT '工作流ID',
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    agent_role VARCHAR(50) NOT NULL COMMENT 'Agent角色',
    input_text TEXT COMMENT '输入内容',
    output_text TEXT COMMENT '输出内容',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
    error_message TEXT COMMENT '错误信息',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    started_at DATETIME,
    ended_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_workflow_session (workflow_id, session_id)
);

CREATE TABLE IF NOT EXISTS agent_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Agent角色名称',
    display_name VARCHAR(100) COMMENT '显示名称',
    description VARCHAR(500) COMMENT '描述',
    tools TEXT COMMENT '工具列表(JSON)',
    system_prompt TEXT COMMENT '系统提示词',
    priority INT DEFAULT 0 COMMENT '优先级',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/inactive',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default agents
INSERT INTO agent_registry (role_name, display_name, description, priority) VALUES
('assistant', '通用助手', '通用对话助手', 0),
('calculator', '计算专家', '擅长数学计算', 1),
('weather', '天气专家', '查询城市天气', 2),
('searcher', '搜索专家', '信息搜索和查找', 3),
('summarizer', '总结专家', '总结和归纳信息', 4)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), description=VALUES(description);
-- Workflow Edge table for DAG connections
CREATE TABLE IF NOT EXISTS agent_workflow_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL COMMENT '所属工作流ID',
    source_node_id BIGINT NOT NULL COMMENT '源节点ID',
    target_node_id BIGINT NOT NULL COMMENT '目标节点ID',
    condition_expression VARCHAR(500) COMMENT '条件表达式（条件分支用）',
    label VARCHAR(100) COMMENT '边标签',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id),
    INDEX idx_source (source_node_id),
    INDEX idx_target (target_node_id)
);

-- Add new columns to agent_workflow_node
ALTER TABLE agent_workflow_node
    ADD COLUMN IF NOT EXISTS node_type VARCHAR(20) NOT NULL DEFAULT 'task' COMMENT '节点类型: start/end/task/condition/parallel/merge' AFTER description,
    ADD COLUMN IF NOT EXISTS pos_x DOUBLE COMMENT '编辑器X坐标' AFTER sort_order,
    ADD COLUMN IF NOT EXISTS pos_y DOUBLE COMMENT '编辑器Y坐标' AFTER pos_x,
    ADD COLUMN IF NOT EXISTS condition_expression VARCHAR(500) COMMENT '条件表达式' AFTER output_mapping,
    ADD COLUMN IF NOT EXISTS parallel_roles VARCHAR(500) COMMENT '并行角色列表(逗号分隔)' AFTER condition_expression;

-- Add collaboration_mode to workflow
ALTER TABLE agent_workflow
    ADD COLUMN IF NOT EXISTS collaboration_mode VARCHAR(50) DEFAULT 'workflow' COMMENT '协作模式' AFTER status;
