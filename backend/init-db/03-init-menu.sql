-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    path VARCHAR(100) COMMENT '路由路径',
    icon VARCHAR(50) COMMENT '图标',
    parent_id BIGINT COMMENT '父菜单ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/inactive',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 插入默认菜单
INSERT INTO sys_menu (name, path, icon, parent_id, sort_order) VALUES
('智能对话', '/chat', 'chat', NULL, 1),
('工作流', '/workflow', 'workflow', NULL, 2),
('执行历史', '/history', 'history', NULL, 3),
('系统管理', NULL, 'setting', NULL, 99)
ON DUPLICATE KEY UPDATE name=VALUES(name), status='active';
