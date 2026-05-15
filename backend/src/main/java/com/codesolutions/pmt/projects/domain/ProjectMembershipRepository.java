package com.codesolutions.pmt.projects.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {
  @EntityGraph(attributePaths = {"user", "project"})
  Optional<ProjectMembership> findByProjectIdAndUserId(UUID projectId, UUID userId);

  boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

  @EntityGraph(attributePaths = "project")
  List<ProjectMembership> findByUserId(UUID userId);

  @EntityGraph(attributePaths = "user")
  List<ProjectMembership> findByProjectId(UUID projectId);
}
