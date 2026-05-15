import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PmtApiService } from './pmt-api.service';

describe('PmtApiService', () => {
  let service: PmtApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PmtApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls project endpoints with the expected payloads', () => {
    service.listProjects().subscribe();
    expectRequest('GET', '/api/v1/projects').flush([]);

    service.createProject('PMT', 'Description', '2026-05-15').subscribe();
    expectRequest('POST', '/api/v1/projects', {
      name: 'PMT',
      description: 'Description',
      startDate: '2026-05-15'
    }).flush(project());

    service.listMembers('p1').subscribe();
    expectRequest('GET', '/api/v1/projects/p1/members').flush([]);

    service.addMember('p1', 'member@pmt.local', 'MEMBER').subscribe();
    expectRequest('POST', '/api/v1/projects/p1/members', {
      email: 'member@pmt.local',
      role: 'MEMBER'
    }).flush({});

    service.changeRole('p1', 'u1', 'OBSERVER').subscribe();
    expectRequest('PATCH', '/api/v1/projects/p1/members/u1/role', { role: 'OBSERVER' }).flush({});
  });

  it('calls task endpoints with filters and payloads', () => {
    service.listTasks('p1').subscribe();
    expectRequest('GET', '/api/v1/projects/p1/tasks').flush([]);

    service.listTasks('p1', 'DOING').subscribe();
    const filtered = http.expectOne('/api/v1/projects/p1/tasks?status=DOING');
    expect(filtered.request.method).toBe('GET');
    filtered.flush([]);

    service.createTask('p1', 'Task', 'Desc', '2026-05-15', 'HIGH', 'u1').subscribe();
    expectRequest('POST', '/api/v1/projects/p1/tasks', {
      name: 'Task',
      description: 'Desc',
      dueDate: '2026-05-15',
      priority: 'HIGH',
      assigneeId: 'u1'
    }).flush(task());

    service.createTask('p1', 'Task', 'Desc', '2026-05-15', 'LOW', null).subscribe();
    expectRequest('POST', '/api/v1/projects/p1/tasks', {
      name: 'Task',
      description: 'Desc',
      dueDate: '2026-05-15',
      priority: 'LOW',
      assigneeId: null
    }).flush(task());

    service.updateTask('p1', 't1', { status: 'DONE' }).subscribe();
    expectRequest('PATCH', '/api/v1/projects/p1/tasks/t1', { status: 'DONE' }).flush(task());

    service.assignTask('p1', 't1', 'u1').subscribe();
    expectRequest('PATCH', '/api/v1/projects/p1/tasks/t1/assignee', { assigneeId: 'u1' }).flush(task());

    service.taskHistory('p1', 't1').subscribe();
    expectRequest('GET', '/api/v1/projects/p1/tasks/t1/history').flush([]);
  });

  function expectRequest(method: string, url: string, body?: unknown) {
    const request = http.expectOne(url);
    expect(request.request.method).toBe(method);
    if (body !== undefined) {
      expect(request.request.body).toEqual(body);
    }
    return request;
  }
});

function project() {
  return { id: 'p1', name: 'PMT', description: 'Description', startDate: '2026-05-15' };
}

function task() {
  return {
    id: 't1',
    name: 'Task',
    description: 'Desc',
    dueDate: '2026-05-15',
    endDate: null,
    priority: 'HIGH',
    status: 'BACKLOG',
    assignee: null
  };
}
