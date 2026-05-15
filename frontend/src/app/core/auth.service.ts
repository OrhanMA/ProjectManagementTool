import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthResponse, UserProfile } from './api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api/v1/auth';
  private readonly accessTokenSignal = signal<string | null>(null);
  private readonly userSignal = signal<UserProfile | null>(null);

  readonly accessToken = this.accessTokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null && this.accessTokenSignal() !== null);

  constructor(private readonly http: HttpClient) {}

  register(username: string, email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, { username, email, password })
      .pipe(tap((response) => this.storeSession(response)));
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, { email, password })
      .pipe(tap((response) => this.storeSession(response)));
  }

  refresh() {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/refresh`, {})
      .pipe(tap((response) => this.storeSession(response)));
  }

  logout() {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}).pipe(tap(() => this.clearSession()));
  }

  clearSession() {
    this.accessTokenSignal.set(null);
    this.userSignal.set(null);
  }

  private storeSession(response: AuthResponse) {
    this.accessTokenSignal.set(response.accessToken);
    this.userSignal.set(response.user);
  }
}
