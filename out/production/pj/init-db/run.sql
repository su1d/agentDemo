CREATE TABLE IF NOT EXISTS agent_workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    agent_role VARCHAR(50) NOT NULL,
    workflow_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    input_template TEXT,
    output_mapping TEXT,
    timeout_seconds INT DEFAULT 60,
    retry_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id)
);

CREATE TABLE IF NOT EXISTS agent_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'draft',
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT,
    session_id VARCHAR(100) NOT NULL,
    agent_role VARCHAR(50) NOT NULL,
    input_text TEXT,
    output_text TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    error_message TEXT,
    duration_ms BIGINT,
    started_at DATETIME,
    ended_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_workflow_session (workflow_id, session_id)
);

CREATE TABLE IF NOT EXISTS agent_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    description VARCHAR(500),
    tools TEXT,
    system_prompt TEXT,
    priority INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO agent_registry (role_name, display_name, description, priority) VALUES
('assistant', 'TongYongZhuShou', 'TongYongDuiHuaZhuShou', 0),
('calculator', 'JiSuanZhuanJia', 'ShanChangShuXueJiSuan', 1),
('weather', 'TianQiZhuanJia', 'ChaXunChengShiTianQi', 2),
('searcher', 'SouSuoZhuanJia', 'XinXiSouSuoHeChaZhao', 3),
('summarizer', 'ZongJieZhuanJia', 'ZongJieHeGuiNaXinXi', 4)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), description=VALUES(description);

CREATE TABLE IF NOT EXISTS agent_workflow_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    condition_expression VARCHAR(500),
    label VARCHAR(100),
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id),
    INDEX idx_source (source_node_id),
    INDEX idx_target (target_node_id)
);

ALTER TABLE agent_workflow_node
    ADD COLUMN IF NOT EXISTS node_type VARCHAR(20) NOT NULL DEFAULT 'task' AFTER description,
    ADD COLUMN IF NOT EXISTS pos_x DOUBLE AFTER sort_order,
    ADD COLUMN IF NOT EXISTS pos_y DOUBLE AFTER pos_x,
    ADD COLUMN IF NOT EXISTS condition_expression VARCHAR(500) AFTER output_mapping,
    ADD COLUMN IF NOT EXISTS parallel_roles VARCHAR(500) AFTER condition_expression;

ALTER TABLE agent_workflow
    ADD COLUMN IF NOT EXISTS collaboration_mode VARCHAR(50) DEFAULT 'workflow' AFTER status;