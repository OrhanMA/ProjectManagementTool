import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  it('adds credentials and bearer token when available', () => {
    const token = signal('token-123');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { accessToken: token.asReadonly() } }
      ]
    });

    TestBed.inject(HttpClient).get('/api/v1/projects').subscribe();

    const request = TestBed.inject(HttpTestingController).expectOne('/api/v1/projects');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-123');
    request.flush([]);
  });

  it('keeps credentials without authorization for anonymous calls', () => {
    const token = signal<string | null>(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { accessToken: token.asReadonly() } }
      ]
    });

    TestBed.inject(HttpClient).post('/api/v1/auth/login', {}).subscribe();

    const request = TestBed.inject(HttpTestingController).expectOne('/api/v1/auth/login');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
