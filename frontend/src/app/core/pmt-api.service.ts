import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  Project,
  ProjectMember,
  ProjectRole,
  ProjectTask,
  TaskHistoryEntry,
  TaskPriority,
  TaskStatus
} from './api.models';

@Injectable({ providedIn: 'root' })
export class PmtApiService {
  private readonly apiUrl = '/api/v1';

  constructor(private readonly http: HttpClient) {}

  listProjects() {
    return this.http.get<Project[]>(`${this.apiUrl}/projects`);
  }

  createProject(name: string, description: string, startDate: string) {
    return this.http.post<Project>(`${this.apiUrl}/projects`, { name, description, startDate });
  }

  listMembers(projectId: string) {
    return this.http.get<ProjectMember[]>(`${this.apiUrl}/projects/${projectId}/members`);
  }

  addMember(projectId: string, email: string, role: ProjectRole) {
    return this.http.post<ProjectMember>(`${this.apiUrl}/projects/${projectId}/members`, { email, role });
  }

  changeRole(projectId: string, userId: string, role: ProjectRole) {
    return this.http.patch<ProjectMember>(`${this.apiUrl}/projects/${projectId}/members/${userId}/role`, { role });
  }

  listTasks(projectId: string, status?: TaskStatus) {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<ProjectTask[]>(`${this.apiUrl}/projects/${projectId}/tasks`, { params });
  }

  createTask(
    projectId: string,
    name: string,
    description: string,
    dueDate: string,
    priority: TaskPriority,
    assigneeId: string | null
  ) {
    return this.http.post<ProjectTask>(`${this.apiUrl}/projects/${projectId}/tasks`, {
      name,
      description,
      dueDate,
      priority,
      assigneeId: assigneeId || null
    });
  }

  updateTask(projectId: string, taskId: string, updates: Partial<ProjectTask>) {
    return this.http.patch<ProjectTask>(`${this.apiUrl}/projects/${projectId}/tasks/${taskId}`, updates);
  }

  assignTask(projectId: string, taskId: string, assigneeId: string) {
    return this.http.patch<ProjectTask>(`${this.apiUrl}/projects/${projectId}/tasks/${taskId}/assignee`, {
      assigneeId
    });
  }

  taskHistory(projectId: string, taskId: string) {
    return this.http.get<TaskHistoryEntry[]>(`${this.apiUrl}/projects/${projectId}/tasks/${taskId}/history`);
  }
}
