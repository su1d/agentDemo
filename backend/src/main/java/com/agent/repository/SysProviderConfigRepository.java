package com.agent.repository;

import com.agent.entity.SysProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysProviderConfigRepository extends JpaRepository<SysProviderConfig, Long> {
    Optional<SysProviderConfig> findByProviderKey(String providerKey);
}
