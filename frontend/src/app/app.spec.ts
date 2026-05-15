import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { App } from './app';
import { AuthService } from './core/auth.service';
import { PmtApiService } from './core/pmt-api.service';

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let auth: FakeAuthService;
  let api: FakePmtApiService;

  beforeEach(async () => {
    auth = new FakeAuthService();
    api = new FakePmtApiService();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: auth },
        { provide: PmtApiService, useValue: api },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  });

  it('renders the public landing page before authentication', () => {
    expect(fixture.nativeElement.textContent).toContain('Gestion de projet');
    expect(fixture.nativeElement.textContent).toContain('Connexion');
    expect(fixture.componentInstance.dashboardTitle()).toBe('Aucun projet sélectionné');
  });

  it('loads projects after login', () => {
    fixture.componentInstance.loginForm.setValue({
      email: 'alice.admin@pmt.local',
      password: 'Password123!',
    });

    fixture.componentInstance.login();

    expect(auth.loggedIn).toBe(true);
    expect(fixture.componentInstance.projects()).toHaveLength(1);
    expect(fixture.componentInstance.selectedProject()?.name).toBe('Refonte portail client');
    expect(fixture.componentInstance.dashboardTitle()).toBe('Refonte portail client');
  });

  it('keeps an empty workspace when the user has no project yet', () => {
    api.emptyProjects = true;

    fixture.componentInstance.login();

    expect(fixture.componentInstance.projects()).toHaveLength(0);
    expect(fixture.componentInstance.selectedProject()).toBeNull();
  });

  it('creates a project and selects it', () => {
    auth.authenticate();
    fixture.componentInstance.projectForm.setValue({
      name: 'Nouvelle plateforme',
      description: 'Projet créé depuis le test',
      startDate: '2026-05-15',
    });

    fixture.componentInstance.createProject();

    expect(api.createdProjectName).toBe('Nouvelle plateforme');
    expect(fixture.componentInstance.selectedProject()?.name).toBe('Nouvelle plateforme');
  });

  it('filters tasks by status and formats labels', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);

    expect(fixture.componentInstance.tasksByStatus('DOING')).toHaveLength(1);
    expect(fixture.componentInstance.statusIcon('REVIEW')).toBe('rate_review');
    expect(fixture.componentInstance.priorityClass('HIGH')).toBe('priority-high');
    expect(fixture.componentInstance.memberInitials(api.member)).toBe('MM');
    expect(fixture.componentInstance.memberInitials(null)).toBe('NA');
    expect(fixture.componentInstance.assigneeInitials(api.task)).toBe('MM');
    expect(fixture.componentInstance.assigneeInitials({ ...api.task, assignee: null })).toBe('NA');
    expect(fixture.componentInstance.statusLabel('TODO')).toBe('À faire');
    expect(fixture.componentInstance.priorityLabel('URGENT')).toBe('Urgent');
    expect(fixture.componentInstance.roleLabel('OBSERVER')).toBe('Observateur');
  });

  it('shows an error when login fails', () => {
    auth.failLogin = true;

    fixture.componentInstance.login();

    expect(fixture.componentInstance.error()).toContain('Connexion impossible');
  });

  it('registers a new user and loads the workspace', () => {
    fixture.componentInstance.authMode.set('register');
    fixture.componentInstance.registerForm.setValue({
      username: 'new.user',
      email: 'new.user@pmt.local',
      password: 'Password123!',
    });

    fixture.componentInstance.register();

    expect(auth.registered).toBe(true);
    expect(fixture.componentInstance.projects()).toHaveLength(1);
  });

  it('does not register invalid data and reports registration errors', () => {
    fixture.componentInstance.registerForm.controls.email.setValue('invalid');
    fixture.componentInstance.register();
    expect(auth.registered).toBe(false);

    fixture.componentInstance.registerForm.setValue({
      username: 'new.user',
      email: 'new.user@pmt.local',
      password: 'Password123!',
    });
    auth.failRegister = true;
    fixture.componentInstance.register();

    expect(fixture.componentInstance.error()).toContain('Inscription impossible');
  });

  it('ignores invalid forms before sending API calls', () => {
    fixture.componentInstance.loginForm.controls.email.setValue('bad-email');
    fixture.componentInstance.login();
    expect(auth.loggedIn).toBe(false);

    auth.authenticate();
    fixture.componentInstance.projectForm.controls.name.setValue('');
    fixture.componentInstance.createProject();
    expect(api.createdProjectName).toBe('');

    fixture.componentInstance.memberForm.controls.email.setValue('bad-email');
    fixture.componentInstance.addMember();
    expect(api.addedMember).toBe(false);

    fixture.componentInstance.taskForm.controls.name.setValue('');
    fixture.componentInstance.createTask();
    expect(api.createdTask).toBe(false);
  });

  it('adds members, creates tasks and updates the board', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);

    fixture.componentInstance.addMember();
    expect(api.addedMember).toBe(true);
    expect(fixture.componentInstance.members()).toHaveLength(3);

    fixture.componentInstance.taskForm.setValue({
      name: 'Livrer le dashboard',
      description: 'Construire la vue kanban',
      dueDate: '2026-05-30',
      priority: 'URGENT',
      assigneeId: 'u2',
    });
    fixture.componentInstance.createTask();

    expect(api.createdTask).toBe(true);
    expect(fixture.componentInstance.tasks()).toHaveLength(2);
  });

  it('moves, assigns and opens task history', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);
    const task = fixture.componentInstance.tasks()[0];

    fixture.componentInstance.moveTask(task, 'DONE');
    expect(api.updatedTaskStatus).toBe('DONE');

    const updated = fixture.componentInstance.tasks()[0];
    fixture.componentInstance.dropTask({
      item: { data: updated },
      container: { data: 'REVIEW' },
    } as any);
    expect(api.updatedTaskStatus).toBe('REVIEW');

    fixture.componentInstance.assignTask(updated, 'u1');
    expect(api.assignedUserId).toBe('u1');

    fixture.componentInstance.openHistory(updated);
    expect(fixture.componentInstance.selectedTask()?.id).toBe(updated.id);
    expect(fixture.componentInstance.history()).toHaveLength(1);
  });

  it('opens, edits and closes task details', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);
    const task = fixture.componentInstance.tasks()[0];

    fixture.componentInstance.openTaskDetail(task);

    expect(fixture.componentInstance.selectedTask()?.id).toBe(task.id);
    expect(fixture.componentInstance.taskEditForm.getRawValue()).toEqual({
      name: task.name,
      description: task.description,
      dueDate: task.dueDate,
      endDate: '',
      priority: task.priority,
      status: task.status,
    });

    fixture.componentInstance.taskEditForm.setValue({
      name: 'Concevoir le modèle final',
      description: 'Préparer le schéma relationnel validé',
      dueDate: '2026-05-29',
      endDate: '2026-06-01',
      priority: 'URGENT',
      status: 'REVIEW',
    });
    fixture.componentInstance.saveTaskDetails();

    expect(api.lastTaskUpdate).toEqual({
      name: 'Concevoir le modèle final',
      description: 'Préparer le schéma relationnel validé',
      dueDate: '2026-05-29',
      endDate: '2026-06-01',
      priority: 'URGENT',
      status: 'REVIEW',
    });
    expect(fixture.componentInstance.selectedTask()?.name).toBe('Concevoir le modèle final');

    fixture.componentInstance.closeTaskDetail();
    expect(fixture.componentInstance.selectedTask()).toBeNull();
    expect(fixture.componentInstance.history()).toHaveLength(0);
  });

  it('does not call APIs when project or value is missing', () => {
    fixture.componentInstance.addMember();
    fixture.componentInstance.createTask();
    fixture.componentInstance.moveTask(api.task, api.task.status);
    fixture.componentInstance.dropTask({
      item: { data: null },
      container: { data: 'DONE' },
    } as any);
    fixture.componentInstance.assignTask(api.task, '');
    fixture.componentInstance.openHistory(api.task);
    fixture.componentInstance.saveTaskDetails();

    expect(api.addedMember).toBe(false);
    expect(api.createdTask).toBe(false);
    expect(api.historyLoaded).toBe(false);
    expect(api.lastTaskUpdate).toBeNull();
  });

  it('resets workspace on logout', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);

    fixture.componentInstance.logout();

    expect(fixture.componentInstance.projects()).toHaveLength(0);
    expect(fixture.componentInstance.selectedProject()).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('loads projects from a refresh cookie when available', async () => {
    auth.refreshSucceeds = true;
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.componentInstance.projects()).toHaveLength(1);
  });

  it('handles API errors with readable messages', () => {
    auth.authenticate();

    api.failListProjects = true;
    fixture.componentInstance.login();
    expect(fixture.componentInstance.error()).toContain('Chargement des projets impossible');

    api.failListProjects = false;
    api.failCreateProject = true;
    fixture.componentInstance.projectForm.setValue({
      name: 'Projet',
      description: 'Description',
      startDate: '2026-05-15',
    });
    fixture.componentInstance.createProject();
    expect(fixture.componentInstance.error()).toContain('Création du projet impossible');

    api.failMembers = true;
    api.failTasks = true;
    fixture.componentInstance.selectProject(api.project);
    expect(fixture.componentInstance.error()).toContain('Chargement des tâches impossible');

    api.failAddMember = true;
    fixture.componentInstance.memberForm.setValue({ email: 'member@pmt.local', role: 'MEMBER' });
    fixture.componentInstance.addMember();
    expect(fixture.componentInstance.error()).toContain('Ajout du membre impossible');

    api.failCreateTask = true;
    fixture.componentInstance.taskForm.setValue({
      name: 'Tâche',
      description: 'Description',
      dueDate: '2026-05-20',
      priority: 'HIGH',
      assigneeId: 'u2',
    });
    fixture.componentInstance.createTask();
    expect(fixture.componentInstance.error()).toContain('Création de la tâche impossible');
  });

  it('handles task mutation errors and logout errors', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);
    const task = fixture.componentInstance.tasks()[0];

    api.failUpdateTask = true;
    fixture.componentInstance.moveTask(task, 'DONE');
    expect(fixture.componentInstance.error()).toContain('Mise à jour de la tâche impossible');

    fixture.componentInstance.openTaskDetail(task);
    fixture.componentInstance.taskEditForm.patchValue({ name: 'Nom modifié' });
    fixture.componentInstance.saveTaskDetails();
    expect(fixture.componentInstance.error()).toContain('Mise à jour de la tâche impossible');

    api.failAssignTask = true;
    fixture.componentInstance.assignTask(task, 'u1');
    expect(fixture.componentInstance.error()).toContain('Assignation de la tâche impossible');

    api.failHistory = true;
    fixture.componentInstance.openHistory(task);
    expect(fixture.componentInstance.error()).toContain("Chargement de l'historique impossible");

    auth.failLogout = true;
    fixture.componentInstance.logout();
    expect(fixture.componentInstance.selectedProject()).toBeNull();
  });

  it('refreshes history when the selected task is updated', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);
    const task = fixture.componentInstance.tasks()[0];
    fixture.componentInstance.openHistory(task);
    api.historyLoaded = false;

    fixture.componentInstance.moveTask(task, 'DONE');

    expect(api.historyLoaded).toBe(true);
  });

  it('replaces only the updated task in the local board', () => {
    auth.authenticate();
    fixture.componentInstance.selectProject(api.project);
    fixture.componentInstance.tasks.set([
      api.task,
      { ...api.task, id: 't-other', name: 'Autre tâche', status: 'TODO' },
    ]);

    fixture.componentInstance.moveTask(api.task, 'DONE');

    expect(fixture.componentInstance.tasks().find((task) => task.id === 't-other')?.status).toBe(
      'TODO',
    );
  });

  it('toggles between kanban and list views', () => {
    expect(fixture.componentInstance.boardView()).toBe('kanban');

    fixture.componentInstance.boardView.set('list');

    expect(fixture.componentInstance.boardView()).toBe('list');
  });
});

class FakeAuthService {
  private readonly userSignal = signal(
    null as { id: string; username: string; email: string } | null,
  );
  private readonly tokenSignal = signal(null as string | null);
  readonly user = this.userSignal.asReadonly();
  readonly accessToken = this.tokenSignal.asReadonly();
  readonly isAuthenticated = () => this.userSignal() !== null && this.tokenSignal() !== null;
  loggedIn = false;
  registered = false;
  failLogin = false;
  failRegister = false;
  failLogout = false;
  refreshSucceeds = false;

  authenticate() {
    this.userSignal.set({ id: 'u1', username: 'alice.admin', email: 'alice.admin@pmt.local' });
    this.tokenSignal.set('token');
  }

  refresh() {
    if (this.refreshSucceeds) {
      this.authenticate();
      return of({});
    }
    return throwError(() => new Error('no cookie'));
  }

  login() {
    if (this.failLogin) {
      return throwError(() => new Error('bad credentials'));
    }
    this.loggedIn = true;
    this.authenticate();
    return of({});
  }

  register() {
    if (this.failRegister) {
      return throwError(() => new Error('register failed'));
    }
    this.registered = true;
    this.authenticate();
    return of({});
  }

  logout() {
    if (this.failLogout) {
      return throwError(() => new Error('logout failed'));
    }
    this.clearSession();
    return of(undefined);
  }

  clearSession() {
    this.userSignal.set(null);
    this.tokenSignal.set(null);
  }
}

class FakePmtApiService {
  readonly project = {
    id: 'p1',
    name: 'Refonte portail client',
    description: 'Projet demo',
    startDate: '2026-05-15',
  };
  readonly task = {
    id: 't1',
    name: 'Concevoir le modèle',
    description: 'Préparer le schéma',
    dueDate: '2026-05-22',
    endDate: null,
    priority: 'HIGH',
    status: 'DOING',
    assignee: { id: 'u2', username: 'marc.member', email: 'marc.member@pmt.local' },
  } as const;
  readonly member = {
    userId: 'u2',
    username: 'marc.member',
    email: 'marc.member@pmt.local',
    role: 'MEMBER',
  } as const;
  createdProjectName = '';
  addedMember = false;
  createdTask = false;
  updatedTaskStatus = '';
  lastTaskUpdate: Record<string, unknown> | null = null;
  assignedUserId = '';
  historyLoaded = false;
  emptyProjects = false;
  failListProjects = false;
  failCreateProject = false;
  failMembers = false;
  failTasks = false;
  failAddMember = false;
  failCreateTask = false;
  failUpdateTask = false;
  failAssignTask = false;
  failHistory = false;

  listProjects() {
    if (this.failListProjects) {
      return throwError(() => new Error('projects failed'));
    }
    if (this.emptyProjects) {
      return of([]);
    }
    return of([this.project]);
  }

  createProject(name: string, description: string, startDate: string) {
    if (this.failCreateProject) {
      return throwError(() => new Error('create project failed'));
    }
    this.createdProjectName = name;
    return of({ id: 'p2', name, description, startDate });
  }

  listMembers() {
    if (this.failMembers) {
      return throwError(() => new Error('members failed'));
    }
    return of([
      {
        userId: 'u1',
        username: 'alice.admin',
        email: 'alice.admin@pmt.local',
        role: 'ADMINISTRATOR',
      },
      this.member,
    ]);
  }

  addMember() {
    if (this.failAddMember) {
      return throwError(() => new Error('add member failed'));
    }
    this.addedMember = true;
    return of({
      userId: 'u3',
      username: 'new.member',
      email: 'new.member@pmt.local',
      role: 'MEMBER',
    });
  }

  listTasks() {
    if (this.failTasks) {
      return throwError(() => new Error('tasks failed'));
    }
    return of([this.task]);
  }

  createTask() {
    if (this.failCreateTask) {
      return throwError(() => new Error('create task failed'));
    }
    this.createdTask = true;
    return of({
      id: 't2',
      name: 'Nouvelle tâche',
      description: 'Description',
      dueDate: '2026-05-22',
      endDate: null,
      priority: 'MEDIUM',
      status: 'BACKLOG',
      assignee: null,
    });
  }

  updateTask(_projectId: string, _taskId: string, updates: Record<string, unknown>) {
    if (this.failUpdateTask) {
      return throwError(() => new Error('update task failed'));
    }
    this.lastTaskUpdate = updates;
    this.updatedTaskStatus = String(updates['status'] || '');
    return of({
      ...this.task,
      ...updates,
      status: updates['status'] || this.task.status,
    });
  }

  assignTask(_projectId: string, _taskId: string, assigneeId: string) {
    if (this.failAssignTask) {
      return throwError(() => new Error('assign task failed'));
    }
    this.assignedUserId = assigneeId;
    return of({
      ...this.task,
      assignee: { id: assigneeId, username: 'alice.admin', email: 'alice.admin@pmt.local' },
    });
  }

  taskHistory() {
    if (this.failHistory) {
      return throwError(() => new Error('history failed'));
    }
    this.historyLoaded = true;
    return of([
      {
        fieldName: 'status',
        oldValue: 'DOING',
        newValue: 'DONE',
        actorEmail: 'alice.admin@pmt.local',
        changedAt: '2026-05-15T10:00:00Z',
      },
    ]);
  }
}
