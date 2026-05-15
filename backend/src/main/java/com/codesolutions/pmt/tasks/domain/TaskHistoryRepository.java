package com.codesolutions.pmt.tasks.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntry, UUID> {
  @EntityGraph(attributePaths = "actor")
  List<TaskHistoryEntry> findByTaskIdOrderByChangedAtDesc(UUID taskId);
}
