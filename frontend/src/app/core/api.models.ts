export type ProjectRole = 'ADMINISTRATOR' | 'MEMBER' | 'OBSERVER';
export type TaskStatus = 'BACKLOG' | 'TODO' | 'DOING' | 'REVIEW' | 'DONE';
export type TaskPriority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
}

export interface AuthResponse {
  accessToken: string;
  user: UserProfile;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  startDate: string;
}

export interface ProjectMember {
  userId: string;
  username: string;
  email: string;
  role: ProjectRole;
}

export interface TaskAssignee {
  id: string;
  username: string;
  email: string;
}

export interface ProjectTask {
  id: string;
  name: string;
  description: string;
  dueDate: string;
  endDate: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  assignee: TaskAssignee | null;
}

export interface TaskHistoryEntry {
  fieldName: string;
  oldValue: string | null;
  newValue: string | null;
  actorEmail: string;
  changedAt: string;
}
