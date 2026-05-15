import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('registers and stores the returned session', () => {
    service.register('alice', 'alice@pmt.local', 'Password123!').subscribe();

    const request = http.expectOne('/api/v1/auth/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      username: 'alice',
      email: 'alice@pmt.local',
      password: 'Password123!'
    });
    request.flush(authResponse());

    expect(service.accessToken()).toBe('access-token');
    expect(service.user()?.email).toBe('alice@pmt.local');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('logs in, refreshes and clears the session', () => {
    service.login('alice@pmt.local', 'Password123!').subscribe();
    http.expectOne('/api/v1/auth/login').flush(authResponse());

    service.refresh().subscribe();
    http.expectOne('/api/v1/auth/refresh').flush(authResponse('fresh-token'));

    expect(service.accessToken()).toBe('fresh-token');

    service.logout().subscribe();
    http.expectOne('/api/v1/auth/logout').flush(null);

    expect(service.accessToken()).toBeNull();
    expect(service.user()).toBeNull();
  });

  it('clears the local session without an HTTP call', () => {
    service.login('alice@pmt.local', 'Password123!').subscribe();
    http.expectOne('/api/v1/auth/login').flush(authResponse());

    service.clearSession();

    expect(service.isAuthenticated()).toBe(false);
  });
});

function authResponse(accessToken = 'access-token') {
  return {
    accessToken,
    user: { id: 'u1', username: 'alice', email: 'alice@pmt.local' }
  };
}
