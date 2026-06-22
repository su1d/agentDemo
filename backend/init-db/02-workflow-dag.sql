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
