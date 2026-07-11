package com.bugtracker.repository;

import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BugRepository extends JpaRepository<Bug, Long> {
    List<Bug> findByAssignedTo_Id(Long userId);
    List<Bug> findByReportedBy_Id(Long userId);
    List<Bug> findByStatus(BugStatus status);
}
