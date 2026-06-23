package com.agent.config;

import com.agent.entity.SysProviderConfig;
import com.agent.entity.SysMenu;
import com.agent.repository.SysMenuRepository;
import com.agent.repository.SysProviderConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SysMenuRepository menuRepository;
    private final SysProviderConfigRepository providerConfigRepository;
    private final String openaiApiKey;
    private final String openaiBaseUrl;
    private final String openaiModel;
    private final String deepseekApiKey;
    private final String deepseekBaseUrl;
    private final String deepseekModel;

    public DataInitializer(SysMenuRepository menuRepository, SysProviderConfigRepository providerConfigRepository,
                           @Value("${openai.api-key:}") String openaiApiKey,
                           @Value("${openai.base-url:https://api.deepseek.com}") String openaiBaseUrl,
                           @Value("${openai.model:deepseek-chat}") String openaiModel,
                           @Value("${deepseek.api-key:}") String deepseekApiKey,
                           @Value("${deepseek.base-url:https://api.deepseek.com}") String deepseekBaseUrl,
                           @Value("${deepseek.model:deepseek-chat}") String deepseekModel) {
        this.menuRepository = menuRepository;
        this.providerConfigRepository = providerConfigRepository;
        this.openaiApiKey = openaiApiKey;
        this.openaiBaseUrl = openaiBaseUrl;
        this.openaiModel = openaiModel;
        this.deepseekApiKey = deepseekApiKey;
        this.deepseekBaseUrl = deepseekBaseUrl;
        this.deepseekModel = deepseekModel;
    }

    @Override
    public void run(String... args) {
        initMenus();
        initDefaultProviders();
    }

    private void initMenus() {
        if (menuRepository.count() > 0) {
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
            return;
        }

        providerConfigRepository.save(new SysProviderConfig(
            "openai", "OpenAI",
            openaiApiKey != null && !openaiApiKey.isEmpty() ? openaiApiKey : "",
            openaiBaseUrl,
            openaiModel,
            "gpt-4o-mini,gpt-4o,gpt-4-turbo,gpt-3.5-turbo",
            true
        ));

        providerConfigRepository.save(new SysProviderConfig(
            "deepseek", "DeepSeek",
            deepseekApiKey != null && !deepseekApiKey.isEmpty() ? deepseekApiKey : "",
            deepseekBaseUrl,
            deepseekModel,
            "deepseek-chat,deepseek-reasoner",
            true
        ));

        System.out.println("[Init] Default providers created.");
    }

}
