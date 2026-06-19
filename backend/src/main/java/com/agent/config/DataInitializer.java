package com.agent.config;

import com.agent.entity.SysMenu;
import com.agent.repository.SysMenuRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SysMenuRepository menuRepository;

    public DataInitializer(SysMenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Override
    public void run(String... args) {
        if (menuRepository.count() > 0) return;

        SysMenu chat = menuRepository.save(new SysMenu("智能对话", "/chat", "chat", null, 1));
        SysMenu workflow = menuRepository.save(new SysMenu("工作流", "/workflow", "workflow", null, 2));
        SysMenu history = menuRepository.save(new SysMenu("执行历史", "/history", "history", null, 3));
        SysMenu system = menuRepository.save(new SysMenu("系统管理", null, "setting", null, 99));

        menuRepository.save(new SysMenu("菜单管理", "/menu-manager", "setting", system.getId(), 1));

        System.out.println("[Init] Default menus created.");
    }
}
