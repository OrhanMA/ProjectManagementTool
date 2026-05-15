package com.codesolutions.pmt.projects.api;

import com.codesolutions.pmt.projects.application.ProjectService;
import com.codesolutions.pmt.projects.domain.Project;
import com.codesolutions.pmt.projects.domain.ProjectMembership;
import com.codesolutions.pmt.shared.domain.ProjectRole;
import com.codesolutions.pmt.shared.security.SecurityContextReader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
  private final ProjectService projectService;
  private final SecurityContextReader security;

  public ProjectController(ProjectService projectService, SecurityContextReader security) {
    this.projectService = projectService;
    this.security = security;
  }

  @PostMapping
  ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
    Project project =
        projectService.createProject(
            security.currentUser().id(),
            request.name(),
            request.description(),
            request.startDate());
    return ProjectResponse.from(project);
  }

  @GetMapping
  List<ProjectResponse> list() {
    return projectService.listProjects(security.currentUser().id()).stream()
        .map(ProjectResponse::from)
        .toList();
  }

  @GetMapping("/{projectId}")
  ProjectResponse get(@PathVariable UUID projectId) {
    return ProjectResponse.from(projectService.getProject(security.currentUser().id(), projectId));
  }

  @GetMapping("/{projectId}/members")
  List<MemberResponse> members(@PathVariable UUID projectId) {
    return projectService.listMembers(security.currentUser().id(), projectId).stream()
        .map(MemberResponse::from)
        .toList();
  }

  @PostMapping("/{projectId}/members")
  MemberResponse addMember(
      @PathVariable UUID projectId, @Valid @RequestBody AddMemberRequest request) {
    return MemberResponse.from(
        projectService.addMember(
            security.currentUser().id(), projectId, request.email(), request.role()));
  }

  @PatchMapping("/{projectId}/members/{userId}/role")
  MemberResponse changeRole(
      @PathVariable UUID projectId,
      @PathVariable UUID userId,
      @Valid @RequestBody ChangeRoleRequest request) {
    return MemberResponse.from(
        projectService.changeMemberRole(
            security.currentUser().id(), projectId, userId, request.role()));
  }

  public record CreateProjectRequest(
      @NotBlank @Size(max = 140) String name,
      @NotBlank @Size(max = 1000) String description,
      @NotNull LocalDate startDate) {}

  public record AddMemberRequest(@NotBlank @Email String email, @NotNull ProjectRole role) {}

  public record ChangeRoleRequest(@NotNull ProjectRole role) {}

  public record ProjectResponse(UUID id, String name, String description, LocalDate startDate) {
    static ProjectResponse from(Project project) {
      return new ProjectResponse(
          project.id(), project.name(), project.description(), project.startDate());
    }
  }

  public record MemberResponse(UUID userId, String username, String email, ProjectRole role) {
    static MemberResponse from(ProjectMembership membership) {
      return new MemberResponse(
          membership.user().id(),
          membership.user().username(),
          membership.user().email(),
          membership.role());
    }
  }
}
