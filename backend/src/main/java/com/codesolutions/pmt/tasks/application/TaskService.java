package com.codesolutions.pmt.tasks.application;

import com.codesolutions.pmt.notifications.application.EmailNotificationService;
import com.codesolutions.pmt.projects.application.ProjectService;
import com.codesolutions.pmt.projects.domain.Project;
import com.codesolutions.pmt.projects.domain.ProjectRepository;
import com.codesolutions.pmt.shared.domain.BusinessException;
import com.codesolutions.pmt.shared.domain.TaskPriority;
import com.codesolutions.pmt.shared.domain.TaskStatus;
import com.codesolutions.pmt.tasks.domain.Task;
import com.codesolutions.pmt.tasks.domain.TaskHistoryEntry;
import com.codesolutions.pmt.tasks.domain.TaskHistoryRepository;
import com.codesolutions.pmt.tasks.domain.TaskRepository;
import com.codesolutions.pmt.users.domain.UserAccount;
import com.codesolutions.pmt.users.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
  private final TaskRepository tasks;
  private final TaskHistoryRepository history;
  private final ProjectRepository projects;
  private final UserRepository users;
  private final ProjectService projectService;
  private final EmailNotificationService emailNotificationService;
  private final Clock clock;

  public TaskService(
      TaskRepository tasks,
      TaskHistoryRepository history,
      ProjectRepository projects,
      UserRepository users,
      ProjectService projectService,
      EmailNotificationService emailNotificationService,
      Clock clock) {
    this.tasks = tasks;
    this.history = history;
    this.projects = projects;
    this.users = users;
    this.projectService = projectService;
    this.emailNotificationService = emailNotificationService;
    this.clock = clock;
  }

  @Transactional
  public Task createTask(
      UUID currentUserId,
      UUID projectId,
      String name,
      String description,
      LocalDate dueDate,
      TaskPriority priority,
      UUID assigneeId) {
    projectService.requireCanManageTasks(projectId, currentUserId);
    Project project = project(projectId);
    UserAccount assignee = assigneeId == null ? null : projectMember(projectId, assigneeId);
    Task task =
        tasks.save(
            Task.create(project, name, description, dueDate, priority, assignee, clock.instant()));
    if (assignee != null) {
      history.save(
          TaskHistoryEntry.record(
              task, user(currentUserId), "assignee", null, assignee.email(), clock.instant()));
      emailNotificationService.sendTaskAssigned(assignee, task);
    }
    return task;
  }

  @Transactional(readOnly = true)
  public List<Task> listTasks(UUID currentUserId, UUID projectId, TaskStatus status) {
    projectService.requireMembership(projectId, currentUserId);
    if (status == null) {
      return tasks.findByProjectId(projectId);
    }
    return tasks.findByProjectIdAndStatus(projectId, status);
  }

  @Transactional(readOnly = true)
  public Task getTask(UUID currentUserId, UUID projectId, UUID taskId) {
    projectService.requireMembership(projectId, currentUserId);
    return task(projectId, taskId);
  }

  @Transactional
  public Task updateTask(
      UUID currentUserId, UUID projectId, UUID taskId, UpdateTaskCommand command) {
    projectService.requireCanManageTasks(projectId, currentUserId);
    UserAccount actor = user(currentUserId);
    Task task = task(projectId, taskId);
    Instant now = clock.instant();
    updateIfChanged(
        task, actor, "name", task.name(), command.name(), value -> task.rename(value, now));
    updateIfChanged(
        task,
        actor,
        "description",
        task.description(),
        command.description(),
        value -> task.changeDescription(value, now));
    updateIfChanged(
        task,
        actor,
        "dueDate",
        task.dueDate(),
        command.dueDate(),
        value -> task.changeDueDate(value, now));
    updateIfChanged(
        task,
        actor,
        "endDate",
        task.endDate(),
        command.endDate(),
        value -> task.changeEndDate(value, now));
    updateIfChanged(
        task,
        actor,
        "priority",
        task.priority(),
        command.priority(),
        value -> task.changePriority(value, now));
    updateIfChanged(
        task,
        actor,
        "status",
        task.status(),
        command.status(),
        value -> task.changeStatus(value, now));
    return task;
  }

  @Transactional
  public Task assignTask(UUID currentUserId, UUID projectId, UUID taskId, UUID assigneeId) {
    projectService.requireCanManageTasks(projectId, currentUserId);
    Task task = task(projectId, taskId);
    UserAccount actor = user(currentUserId);
    UserAccount assignee = projectMember(projectId, assigneeId);
    String oldValue = task.assignee() == null ? null : task.assignee().email();
    if (Objects.equals(oldValue, assignee.email())) {
      return task;
    }
    task.assignTo(assignee, clock.instant());
    history.save(
        TaskHistoryEntry.record(
            task, actor, "assignee", oldValue, assignee.email(), clock.instant()));
    emailNotificationService.sendTaskAssigned(assignee, task);
    return task;
  }

  @Transactional(readOnly = true)
  public List<TaskHistoryEntry> history(UUID currentUserId, UUID projectId, UUID taskId) {
    projectService.requireMembership(projectId, currentUserId);
    Task task = task(projectId, taskId);
    return history.findByTaskIdOrderByChangedAtDesc(task.id());
  }

  private <T> void updateIfChanged(
      Task task,
      UserAccount actor,
      String field,
      T oldValue,
      T newValue,
      java.util.function.Consumer<T> update) {
    if (newValue == null || Objects.equals(oldValue, newValue)) {
      return;
    }
    update.accept(newValue);
    history.save(
        TaskHistoryEntry.record(
            task,
            actor,
            field,
            oldValue == null ? null : String.valueOf(oldValue),
            String.valueOf(newValue),
            clock.instant()));
  }

  private Project project(UUID projectId) {
    return projects
        .findById(projectId)
        .orElseThrow(() -> BusinessException.notFound("Projet introuvable."));
  }

  private Task task(UUID projectId, UUID taskId) {
    return tasks
        .findByIdAndProjectId(taskId, projectId)
        .orElseThrow(() -> BusinessException.notFound("Tache introuvable."));
  }

  private UserAccount user(UUID userId) {
    return users
        .findById(userId)
        .orElseThrow(() -> BusinessException.notFound("Utilisateur introuvable."));
  }

  private UserAccount projectMember(UUID projectId, UUID userId) {
    projectService.requireMembership(projectId, userId);
    return user(userId);
  }

  public record UpdateTaskCommand(
      String name,
      String description,
      LocalDate dueDate,
      LocalDate endDate,
      TaskPriority priority,
      TaskStatus status) {}
}
