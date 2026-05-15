package com.codesolutions.pmt.tasks.api;

import com.codesolutions.pmt.shared.domain.TaskPriority;
import com.codesolutions.pmt.shared.domain.TaskStatus;
import com.codesolutions.pmt.shared.security.SecurityContextReader;
import com.codesolutions.pmt.tasks.application.TaskService;
import com.codesolutions.pmt.tasks.domain.Task;
import com.codesolutions.pmt.tasks.domain.TaskHistoryEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
public class TaskController {
  private final TaskService taskService;
  private final SecurityContextReader security;

  public TaskController(TaskService taskService, SecurityContextReader security) {
    this.taskService = taskService;
    this.security = security;
  }

  @PostMapping
  TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateTaskRequest request) {
    return TaskResponse.from(
        taskService.createTask(
            security.currentUser().id(),
            projectId,
            request.name(),
            request.description(),
            request.dueDate(),
            request.priority(),
            request.assigneeId()));
  }

  @GetMapping
  List<TaskResponse> list(
      @PathVariable UUID projectId, @RequestParam(required = false) TaskStatus status) {
    return taskService.listTasks(security.currentUser().id(), projectId, status).stream()
        .map(TaskResponse::from)
        .toList();
  }

  @GetMapping("/{taskId}")
  TaskResponse get(@PathVariable UUID projectId, @PathVariable UUID taskId) {
    return TaskResponse.from(taskService.getTask(security.currentUser().id(), projectId, taskId));
  }

  @PatchMapping("/{taskId}")
  TaskResponse update(
      @PathVariable UUID projectId,
      @PathVariable UUID taskId,
      @Valid @RequestBody UpdateTaskRequest request) {
    return TaskResponse.from(
        taskService.updateTask(
            security.currentUser().id(),
            projectId,
            taskId,
            new TaskService.UpdateTaskCommand(
                request.name(),
                request.description(),
                request.dueDate(),
                request.endDate(),
                request.priority(),
                request.status())));
  }

  @PatchMapping("/{taskId}/assignee")
  TaskResponse assign(
      @PathVariable UUID projectId,
      @PathVariable UUID taskId,
      @Valid @RequestBody AssignTaskRequest request) {
    return TaskResponse.from(
        taskService.assignTask(
            security.currentUser().id(), projectId, taskId, request.assigneeId()));
  }

  @GetMapping("/{taskId}/history")
  List<TaskHistoryResponse> history(@PathVariable UUID projectId, @PathVariable UUID taskId) {
    return taskService.history(security.currentUser().id(), projectId, taskId).stream()
        .map(TaskHistoryResponse::from)
        .toList();
  }

  public record CreateTaskRequest(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Size(max = 2000) String description,
      @NotNull LocalDate dueDate,
      @NotNull TaskPriority priority,
      UUID assigneeId) {}

  public record UpdateTaskRequest(
      @Size(max = 160) String name,
      @Size(max = 2000) String description,
      LocalDate dueDate,
      LocalDate endDate,
      TaskPriority priority,
      TaskStatus status) {}

  public record AssignTaskRequest(@NotNull UUID assigneeId) {}

  public record TaskResponse(
      UUID id,
      String name,
      String description,
      LocalDate dueDate,
      LocalDate endDate,
      TaskPriority priority,
      TaskStatus status,
      AssigneeResponse assignee) {
    static TaskResponse from(Task task) {
      AssigneeResponse assignee =
          task.assignee() == null
              ? null
              : new AssigneeResponse(
                  task.assignee().id(), task.assignee().username(), task.assignee().email());
      return new TaskResponse(
          task.id(),
          task.name(),
          task.description(),
          task.dueDate(),
          task.endDate(),
          task.priority(),
          task.status(),
          assignee);
    }
  }

  public record AssigneeResponse(UUID id, String username, String email) {}

  public record TaskHistoryResponse(
      String fieldName, String oldValue, String newValue, String actorEmail, Instant changedAt) {
    static TaskHistoryResponse from(TaskHistoryEntry entry) {
      return new TaskHistoryResponse(
          entry.fieldName(),
          entry.oldValue(),
          entry.newValue(),
          entry.actor().email(),
          entry.changedAt());
    }
  }
}
