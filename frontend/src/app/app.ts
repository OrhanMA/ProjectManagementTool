import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from './core/auth.service';
import {
  Project,
  ProjectMember,
  ProjectRole,
  ProjectTask,
  TaskHistoryEntry,
  TaskPriority,
  TaskStatus,
} from './core/api.models';
import { PmtApiService } from './core/pmt-api.service';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    DragDropModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    MatTabsModule,
    MatToolbarModule,
    MatTooltipModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly api = inject(PmtApiService);

  readonly statuses: TaskStatus[] = ['BACKLOG', 'TODO', 'DOING', 'REVIEW', 'DONE'];
  readonly priorities: TaskPriority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];
  readonly roles: ProjectRole[] = ['ADMINISTRATOR', 'MEMBER', 'OBSERVER'];

  readonly loading = signal(false);
  readonly authMode = signal<'login' | 'register'>('login');
  readonly projects = signal<Project[]>([]);
  readonly members = signal<ProjectMember[]>([]);
  readonly tasks = signal<ProjectTask[]>([]);
  readonly selectedProject = signal<Project | null>(null);
  readonly selectedTask = signal<ProjectTask | null>(null);
  readonly history = signal<TaskHistoryEntry[]>([]);
  readonly error = signal<string | null>(null);
  readonly boardView = signal<'kanban' | 'list'>('kanban');

  readonly user = this.auth.user;
  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly dashboardTitle = computed(
    () => this.selectedProject()?.name ?? 'Aucun projet sélectionné',
  );
  readonly totalTasks = computed(() => this.tasks().length);
  readonly completedTasks = computed(
    () => this.tasks().filter((task) => task.status === 'DONE').length,
  );
  readonly openTasks = computed(() => this.tasks().filter((task) => task.status !== 'DONE').length);

  readonly loginForm = this.fb.nonNullable.group({
    email: ['alice.admin@pmt.local', [Validators.required, Validators.email]],
    password: ['Password123!', [Validators.required]],
  });

  readonly registerForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  readonly projectForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(140)]],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    startDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
  });

  readonly memberForm = this.fb.nonNullable.group({
    email: ['marc.member@pmt.local', [Validators.required, Validators.email]],
    role: ['MEMBER' as ProjectRole, [Validators.required]],
  });

  readonly taskForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    dueDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
    priority: ['MEDIUM' as TaskPriority, [Validators.required]],
    assigneeId: [null as string | null],
  });

  readonly taskEditForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    dueDate: ['', [Validators.required]],
    endDate: [''],
    priority: ['MEDIUM' as TaskPriority, [Validators.required]],
    status: ['BACKLOG' as TaskStatus, [Validators.required]],
  });

  ngOnInit() {
    this.auth.refresh().subscribe({
      next: () => this.loadProjects(),
      error: () => this.auth.clearSession(),
    });
  }

  login() {
    if (this.loginForm.invalid) {
      return;
    }
    this.withLoading(() =>
      this.auth
        .login(this.loginForm.controls.email.value, this.loginForm.controls.password.value)
        .subscribe({
          next: () => this.loadProjects(),
          error: () => this.showError('Connexion impossible. Vérifiez vos identifiants.'),
        }),
    );
  }

  register() {
    if (this.registerForm.invalid) {
      return;
    }
    const value = this.registerForm.getRawValue();
    this.withLoading(() =>
      this.auth.register(value.username, value.email, value.password).subscribe({
        next: () => this.loadProjects(),
        error: () => this.showError('Inscription impossible. Vérifiez les informations saisies.'),
      }),
    );
  }

  logout() {
    this.auth
      .logout()
      .subscribe({ next: () => this.resetWorkspace(), error: () => this.resetWorkspace() });
  }

  createProject() {
    if (this.projectForm.invalid) {
      return;
    }
    const value = this.projectForm.getRawValue();
    this.api.createProject(value.name, value.description, value.startDate).subscribe({
      next: (project) => {
        this.projectForm.reset({
          name: '',
          description: '',
          startDate: new Date().toISOString().slice(0, 10),
        });
        this.projects.update((projects) => [project, ...projects]);
        this.selectProject(project);
      },
      error: () => this.showError('Création du projet impossible.'),
    });
  }

  selectProject(project: Project) {
    this.selectedProject.set(project);
    this.closeTaskDetail();
    this.loadMembers(project.id);
    this.loadTasks(project.id);
  }

  addMember() {
    const project = this.selectedProject();
    if (!project || this.memberForm.invalid) {
      return;
    }
    const value = this.memberForm.getRawValue();
    this.api.addMember(project.id, value.email, value.role).subscribe({
      next: (member) => this.members.update((members) => [...members, member]),
      error: () => this.showError("Ajout du membre impossible. L'utilisateur doit déjà exister."),
    });
  }

  createTask() {
    const project = this.selectedProject();
    if (!project || this.taskForm.invalid) {
      return;
    }
    const value = this.taskForm.getRawValue();
    this.api
      .createTask(
        project.id,
        value.name!,
        value.description!,
        value.dueDate!,
        value.priority!,
        value.assigneeId,
      )
      .subscribe({
        next: (task) => {
          this.tasks.update((tasks) => [task, ...tasks]);
          this.taskForm.reset({
            name: '',
            description: '',
            dueDate: new Date().toISOString().slice(0, 10),
            priority: 'MEDIUM',
            assigneeId: null,
          });
        },
        error: () => this.showError('Création de la tâche impossible.'),
      });
  }

  moveTask(task: ProjectTask, status: TaskStatus) {
    const project = this.selectedProject();
    if (!project || task.status === status) {
      return;
    }
    this.api.updateTask(project.id, task.id, { status }).subscribe({
      next: (updated) => this.replaceTask(updated),
      error: () => this.showError('Mise à jour de la tâche impossible.'),
    });
  }

  dropTask(event: CdkDragDrop<TaskStatus, TaskStatus, ProjectTask>) {
    const task = event.item.data;
    const status = event.container.data;
    if (!task || !status) {
      return;
    }
    this.moveTask(task, status);
  }

  assignTask(task: ProjectTask, assigneeId: string) {
    const project = this.selectedProject();
    if (!project || !assigneeId) {
      return;
    }
    this.api.assignTask(project.id, task.id, assigneeId).subscribe({
      next: (updated) => this.replaceTask(updated),
      error: () => this.showError('Assignation de la tâche impossible.'),
    });
  }

  openTaskDetail(task: ProjectTask) {
    const project = this.selectedProject();
    if (!project) {
      return;
    }
    this.selectedTask.set(task);
    this.taskEditForm.reset({
      name: task.name,
      description: task.description,
      dueDate: task.dueDate,
      endDate: task.endDate ?? '',
      priority: task.priority,
      status: task.status,
    });
    this.api.taskHistory(project.id, task.id).subscribe({
      next: (entries) => this.history.set(entries),
      error: () => this.showError("Chargement de l'historique impossible."),
    });
  }

  openHistory(task: ProjectTask) {
    this.openTaskDetail(task);
  }

  closeTaskDetail() {
    this.selectedTask.set(null);
    this.history.set([]);
    this.taskEditForm.reset({
      name: '',
      description: '',
      dueDate: '',
      endDate: '',
      priority: 'MEDIUM',
      status: 'BACKLOG',
    });
  }

  saveTaskDetails() {
    const project = this.selectedProject();
    const task = this.selectedTask();
    if (!project || !task || this.taskEditForm.invalid) {
      return;
    }
    const value = this.taskEditForm.getRawValue();
    this.api
      .updateTask(project.id, task.id, {
        name: value.name,
        description: value.description,
        dueDate: value.dueDate,
        endDate: value.endDate || null,
        priority: value.priority,
        status: value.status,
      })
      .subscribe({
        next: (updated) => this.replaceTask(updated),
        error: () => this.showError('Mise à jour de la tâche impossible.'),
      });
  }

  tasksByStatus(status: TaskStatus) {
    return this.tasks().filter((task) => task.status === status);
  }

  statusIcon(status: TaskStatus) {
    const icons: Record<TaskStatus, string> = {
      BACKLOG: 'inventory_2',
      TODO: 'radio_button_unchecked',
      DOING: 'cycle',
      REVIEW: 'rate_review',
      DONE: 'check_circle',
    };
    return icons[status];
  }

  priorityClass(priority: TaskPriority) {
    return `priority-${priority.toLowerCase()}`;
  }

  memberInitials(member: ProjectMember | null | undefined) {
    if (!member) {
      return 'NA';
    }
    return member.username
      .split(/[._\s-]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  assigneeInitials(task: ProjectTask) {
    if (!task.assignee) {
      return 'NA';
    }
    return this.memberInitials({
      userId: task.assignee.id,
      username: task.assignee.username,
      email: task.assignee.email,
      role: 'MEMBER',
    });
  }

  statusLabel(status: TaskStatus) {
    const labels: Record<TaskStatus, string> = {
      BACKLOG: 'Backlog',
      TODO: 'À faire',
      DOING: 'En cours',
      REVIEW: 'Revue',
      DONE: 'Terminé',
    };
    return labels[status];
  }

  priorityLabel(priority: TaskPriority) {
    const labels: Record<TaskPriority, string> = {
      URGENT: 'Urgent',
      HIGH: 'Haute',
      MEDIUM: 'Moyenne',
      LOW: 'Basse',
    };
    return labels[priority];
  }

  roleLabel(role: ProjectRole) {
    const labels: Record<ProjectRole, string> = {
      ADMINISTRATOR: 'Administrateur',
      MEMBER: 'Membre',
      OBSERVER: 'Observateur',
    };
    return labels[role];
  }

  private loadProjects() {
    this.api.listProjects().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        if (projects.length > 0) {
          this.selectProject(projects[0]);
        }
      },
      error: () => this.showError('Chargement des projets impossible.'),
    });
  }

  private loadMembers(projectId: string) {
    this.api.listMembers(projectId).subscribe({
      next: (members) => this.members.set(members),
      error: () => this.showError('Chargement des membres impossible.'),
    });
  }

  private loadTasks(projectId: string) {
    this.api.listTasks(projectId).subscribe({
      next: (tasks) => this.tasks.set(tasks),
      error: () => this.showError('Chargement des tâches impossible.'),
    });
  }

  private replaceTask(updated: ProjectTask) {
    this.tasks.update((tasks) => tasks.map((task) => (task.id === updated.id ? updated : task)));
    if (this.selectedTask()?.id === updated.id) {
      this.openTaskDetail(updated);
    }
  }

  private resetWorkspace() {
    this.auth.clearSession();
    this.projects.set([]);
    this.members.set([]);
    this.tasks.set([]);
    this.selectedProject.set(null);
    this.closeTaskDetail();
  }

  private withLoading(action: () => void) {
    this.loading.set(true);
    this.error.set(null);
    action();
    this.loading.set(false);
  }

  private showError(message: string) {
    this.error.set(message);
  }
}
