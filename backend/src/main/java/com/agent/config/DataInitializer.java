package com.agent.config;

import com.agent.entity.SysMenu;
import com.agent.repository.SysMenuRepository;
import com.agent.repository.SysProviderConfigRepository;
import com.agent.entity.SysProviderConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SysMenuRepository menuRepository;
    private final SysProviderConfigRepository providerConfigRepository;

    public DataInitializer(SysMenuRepository menuRepository, SysProviderConfigRepository providerConfigRepository) {
        this.menuRepository = menuRepository;
        this.providerConfigRepository = providerConfigRepository;
    }

    @Override
    public void run(String... args) {
        initMenus();
        initDefaultProviders();
    }

    private void initMenus() {
        if (menuRepository.count() > 0) {
            // Already have menus — just ensure the provider settings sub-menu exists.
            SysMenu system = menuRepository.findByName("系统管理").orElse(null);
            if (system != null && menuRepository.findByName("LLM 提供商").isEmpty()) {
                menuRepository.save(new SysMenu("LLM 提供商", "/providers", "setting", system.getId(), 2));
                System.out.println("[Init] Added missing 'LLM 提供商' menu.");
            }
            return;
        }

        SysMenu chat = menuRepository.save(new SysMenu("智能对话", "/chat", "chat", null, 1));
        SysMenu workflow = menuRepository.save(new SysMenu("工作流", "/workflow", "workflow", null, 2));
        SysMenu history = menuRepository.save(new SysMenu("执行历史", "/history", "history", null, 3));
        SysMenu system = menuRepository.save(new SysMenu("系统管理", null, "setting", null, 99));

        menuRepository.save(new SysMenu("菜单管理", "/menu-manager", "setting", system.getId(), 1));
        menuRepository.save(new SysMenu("LLM 提供商", "/providers", "setting", system.getId(), 2));

        System.out.println("[Init] Default menus created.");
    }

    private void initDefaultProviders() {
        if (providerConfigRepository.count() > 0) {
            return; // already initialized
        }

        String openaiKey = System.getenv("OPENAI_API_KEY");
        String deepseekKey = System.getenv("DEEPSEEK_API_KEY");

        providerConfigRepository.save(new SysProviderConfig(
            "openai", "OpenAI",
            openaiKey != null ? openaiKey : "",
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            "gpt-4o-mini,gpt-4o,gpt-4-turbo,gpt-3.5-turbo",
            true
        ));

        providerConfigRepository.save(new SysProviderConfig(
            "deepseek", "DeepSeek",
            deepseekKey != null ? deepseekKey : "",
            "https://api.deepseek.com",
            "deepseek-chat",
            "deepseek-chat,deepseek-reasoner",
            true
        ));

        System.out.println("[Init] Default providers created.");
    }

}
