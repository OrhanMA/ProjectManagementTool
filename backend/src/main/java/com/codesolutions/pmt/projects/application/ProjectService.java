package com.codesolutions.pmt.projects.application;

import com.codesolutions.pmt.projects.domain.Project;
import com.codesolutions.pmt.projects.domain.ProjectMembership;
import com.codesolutions.pmt.projects.domain.ProjectMembershipRepository;
import com.codesolutions.pmt.projects.domain.ProjectRepository;
import com.codesolutions.pmt.shared.domain.BusinessException;
import com.codesolutions.pmt.shared.domain.ProjectRole;
import com.codesolutions.pmt.users.domain.UserAccount;
import com.codesolutions.pmt.users.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final ProjectMembershipRepository memberships;
  private final UserRepository users;
  private final Clock clock;

  public ProjectService(
      ProjectRepository projects,
      ProjectMembershipRepository memberships,
      UserRepository users,
      Clock clock) {
    this.projects = projects;
    this.memberships = memberships;
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public Project createProject(
      UUID currentUserId, String name, String description, LocalDate startDate) {
    UserAccount owner = user(currentUserId);
    Project project = projects.save(Project.create(name, description, startDate, clock.instant()));
    memberships.save(
        ProjectMembership.create(project, owner, ProjectRole.ADMINISTRATOR, clock.instant()));
    return project;
  }

  @Transactional(readOnly = true)
  public List<Project> listProjects(UUID currentUserId) {
    return memberships.findByUserId(currentUserId).stream()
        .map(ProjectMembership::project)
        .toList();
  }

  @Transactional(readOnly = true)
  public Project getProject(UUID currentUserId, UUID projectId) {
    requireMembership(projectId, currentUserId);
    return project(projectId);
  }

  @Transactional
  public ProjectMembership addMember(
      UUID currentUserId, UUID projectId, String email, ProjectRole role) {
    requireCanManageMembers(projectId, currentUserId);
    Project project = project(projectId);
    UserAccount user =
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> BusinessException.notFound("Utilisateur introuvable."));
    if (memberships.existsByProjectIdAndUserId(projectId, user.id())) {
      throw BusinessException.conflict("Cet utilisateur est deja membre du projet.");
    }
    return memberships.save(ProjectMembership.create(project, user, role, clock.instant()));
  }

  @Transactional
  public ProjectMembership changeMemberRole(
      UUID currentUserId, UUID projectId, UUID userId, ProjectRole role) {
    requireCanManageMembers(projectId, currentUserId);
    ProjectMembership membership =
        memberships
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> BusinessException.notFound("Membre introuvable."));
    membership.changeRole(role);
    return membership;
  }

  @Transactional(readOnly = true)
  public List<ProjectMembership> listMembers(UUID currentUserId, UUID projectId) {
    requireMembership(projectId, currentUserId);
    return memberships.findByProjectId(projectId);
  }

  @Transactional(readOnly = true)
  public ProjectMembership requireMembership(UUID projectId, UUID userId) {
    return memberships
        .findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> BusinessException.forbidden("Acces interdit a ce projet."));
  }

  @Transactional(readOnly = true)
  public void requireCanManageTasks(UUID projectId, UUID userId) {
    ProjectMembership membership = requireMembership(projectId, userId);
    if (!membership.role().canManageTasks()) {
      throw BusinessException.forbidden("Votre role ne permet pas de modifier les taches.");
    }
  }

  @Transactional(readOnly = true)
  public void requireCanManageMembers(UUID projectId, UUID userId) {
    ProjectMembership membership = requireMembership(projectId, userId);
    if (!membership.role().canManageMembers()) {
      throw BusinessException.forbidden("Votre role ne permet pas de gerer les membres.");
    }
  }

  private Project project(UUID projectId) {
    return projects
        .findById(projectId)
        .orElseThrow(() -> BusinessException.notFound("Projet introuvable."));
  }

  private UserAccount user(UUID userId) {
    return users
        .findById(userId)
        .orElseThrow(() -> BusinessException.notFound("Utilisateur introuvable."));
  }
}
