package com.agent.repository;

import com.agent.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    List<SysMenu> findByStatusOrderBySortOrderAsc(String status);
    List<SysMenu> findByParentIdOrderBySortOrderAsc(Long parentId);
    List<SysMenu> findByStatusAndParentIdIsNullOrderBySortOrderAsc(String status);
    Optional<SysMenu> findByName(String name);
}
