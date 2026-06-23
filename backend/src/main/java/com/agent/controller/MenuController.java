package com.agent.controller;

import com.agent.entity.SysMenu;
import com.agent.repository.SysMenuRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "*")
public class MenuController {

    private final SysMenuRepository menuRepository;

    public MenuController(SysMenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * 获取当前用户可见的菜单树
     */
    @GetMapping
    @Cacheable(value = "menus", key = "'menuTree'")
    public ResponseEntity<List<Map<String, Object>>> getMenus() {
        List<SysMenu> topMenus = menuRepository.findByStatusAndParentIdIsNullOrderBySortOrderAsc("active");
        return ResponseEntity.ok(buildMenuTree(topMenus));
    }

    /**
     * 获取所有菜单（管理用）
     */
    @GetMapping("/all")
    @Cacheable(value = "menus", key = "'allMenus'")
    public ResponseEntity<List<SysMenu>> getAllMenus() {
        return ResponseEntity.ok(menuRepository.findByStatusOrderBySortOrderAsc("active"));
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @CacheEvict(value = "menus", allEntries = true)
    public ResponseEntity<SysMenu> createMenu(@RequestBody SysMenu menu) {
        if (menu.getSortOrder() == null) menu.setSortOrder(0);
        return ResponseEntity.ok(menuRepository.save(menu));
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    @CacheEvict(value = "menus", allEntries = true)
    public ResponseEntity<SysMenu> updateMenu(@PathVariable Long id, @RequestBody SysMenu menu) {
        return menuRepository.findById(id)
            .map(existing -> {
                existing.setName(menu.getName());
                existing.setPath(menu.getPath());
                existing.setIcon(menu.getIcon());
                existing.setParentId(menu.getParentId());
                existing.setSortOrder(menu.getSortOrder());
                existing.setStatus(menu.getStatus());
                return ResponseEntity.ok(menuRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @CacheEvict(value = "menus", allEntries = true)
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", menu.getId());
            node.put("name", menu.getName());
            node.put("path", menu.getPath());
            node.put("icon", menu.getIcon());
            node.put("sortOrder", menu.getSortOrder());
            List<SysMenu> children = menuRepository.findByParentIdOrderBySortOrderAsc(menu.getId());
            if (!children.isEmpty()) {
                node.put("children", buildMenuTree(children));
            }
            tree.add(node);
        }
        return tree;
    }
}
