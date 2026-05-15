package com.codesolutions.pmt.tasks.domain;

import com.codesolutions.pmt.shared.domain.TaskStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
  @EntityGraph(attributePaths = "assignee")
  List<Task> findByProjectId(UUID projectId);

  @EntityGraph(attributePaths = "assignee")
  List<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status);

  @EntityGraph(attributePaths = "assignee")
  Optional<Task> findByIdAndProjectId(UUID id, UUID projectId);
}
