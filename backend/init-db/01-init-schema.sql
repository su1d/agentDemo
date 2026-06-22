-- Agent Platform Database Initialization

CREATE TABLE IF NOT EXISTS agent_workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '�ڵ�����',
    description VARCHAR(500) COMMENT '�ڵ�����',
    agent_role VARCHAR(50) NOT NULL COMMENT '����Agent��ɫ',
    workflow_id BIGINT NOT NULL COMMENT '����������ID',
    sort_order INT DEFAULT 0 COMMENT '����˳��',
    input_template TEXT COMMENT '����ģ��',
    output_mapping TEXT COMMENT '���ӳ������(JSON)',
    timeout_seconds INT DEFAULT 60 COMMENT '��ʱʱ��',
    retry_count INT DEFAULT 0 COMMENT '���Դ���',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id)
);

CREATE TABLE IF NOT EXISTS agent_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '����������',
    description VARCHAR(500) COMMENT '����������',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '״̬: draft/active/disabled',
    created_by VARCHAR(100) COMMENT '������',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT COMMENT '������ID',
    session_id VARCHAR(100) NOT NULL COMMENT '�ỰID',
    agent_role VARCHAR(50) NOT NULL COMMENT 'Agent��ɫ',
    input_text TEXT COMMENT '��������',
    output_text TEXT COMMENT '�������',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '״̬: pending/running/success/failed',
    error_message TEXT COMMENT '������Ϣ',
    duration_ms BIGINT COMMENT 'ִ�к�ʱ(����)',
    started_at DATETIME,
    ended_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_workflow_session (workflow_id, session_id)
);

-- �û���
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    display_name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'user',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Agent��ɫ����',
    display_name VARCHAR(100) COMMENT '��ʾ����',
    description VARCHAR(500) COMMENT '����',
    tools TEXT COMMENT '�����б�(JSON)',
    system_prompt TEXT COMMENT 'ϵͳ��ʾ��',
    priority INT DEFAULT 0 COMMENT '���ȼ�',
    status VARCHAR(20) DEFAULT 'active' COMMENT '״̬: active/inactive',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- LLM 提供商配置表
CREATE TABLE IF NOT EXISTS sys_provider_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_key VARCHAR(50) NOT NULL UNIQUE COMMENT '提供商唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '显示名称',
    api_key VARCHAR(500) COMMENT 'API密钥',
    base_url VARCHAR(500) COMMENT 'API基础地址',
    default_model VARCHAR(100) COMMENT '默认模型',
    models VARCHAR(1000) COMMENT '可用模型列表(逗号分隔)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default providers
INSERT INTO sys_provider_config (provider_key, name, api_key, base_url, default_model, models, enabled) VALUES
('openai', 'OpenAI', '', 'https://api.openai.com/v1', 'gpt-4o-mini', 'gpt-4o-mini,gpt-4o,gpt-4-turbo,gpt-3.5-turbo', TRUE),
('deepseek', 'DeepSeek', '', 'https://api.deepseek.com', 'deepseek-chat', 'deepseek-chat,deepseek-reasoner', TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert default agents
INSERT INTO agent_registry (role_name, display_name, description, priority) VALUES
('assistant', 'ͨ������', 'ͨ�öԻ�����', 0),
('calculator', '����ר��', '�ó���ѧ����', 1),
('weather', '����ר��', '��ѯ��������', 2),
('searcher', '����ר��', '��Ϣ�����Ͳ���', 3),
('summarizer', '�ܽ�ר��', '�ܽ�͹�����Ϣ', 4)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), description=VALUES(description);
