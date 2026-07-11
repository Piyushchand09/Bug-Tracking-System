package com.bugtracker.repository;

import com.bugtracker.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByBugIdOrderByTimestampDesc(Long bugId);
}
