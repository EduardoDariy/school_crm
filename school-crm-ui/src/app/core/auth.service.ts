import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { CurrentUser, StaffRole, TokenResponse } from './models';

const TOKEN_KEY = 'crm.token';
const SUBDOMAIN_KEY = 'crm.subdomain';

interface StoredSession {
  accessToken: string;
  expiresAt: string;
  user: CurrentUser;
}

/** Сессия сотрудника: токен в localStorage, текущий пользователь — в сигнале. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<StoredSession | null>(this.restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);

  get token(): string | null {
    return this.session()?.accessToken ?? null;
  }

  /** Поддомен из адреса (school.crm.example → school) или последний введённый. */
  defaultSubdomain(): string {
    const host = window.location.hostname;
    const parts = host.split('.');
    if (parts.length > 2 && !/^\d+$/.test(parts[0])) {
      return parts[0];
    }
    return localStorage.getItem(SUBDOMAIN_KEY) ?? '';
  }

  login(subdomain: string, email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', { subdomain, email, password }).pipe(
      tap(response => {
        localStorage.setItem(SUBDOMAIN_KEY, subdomain.trim().toLowerCase());
        this.store({ accessToken: response.accessToken, expiresAt: response.expiresAt, user: response.user });
      }),
    );
  }

  /** Обновляет данные пользователя (роль могла измениться). */
  refreshUser(): void {
    if (!this.session()) {
      return;
    }
    this.http.get<CurrentUser>('/api/auth/me').subscribe({
      next: user => {
        const current = this.session();
        if (current) {
          this.store({ ...current, user });
        }
      },
    });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/password', { currentPassword, newPassword });
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.session.set(null);
    this.router.navigate(['/login']);
  }

  hasRole(...roles: StaffRole[]): boolean {
    const role = this.user()?.role;
    return role !== undefined && roles.includes(role);
  }

  private store(session: StoredSession): void {
    localStorage.setItem(TOKEN_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private restore(): StoredSession | null {
    try {
      const raw = localStorage.getItem(TOKEN_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as StoredSession;
      if (!session.accessToken || new Date(session.expiresAt).getTime() <= Date.now()) {
        localStorage.removeItem(TOKEN_KEY);
        return null;
      }
      return session;
    } catch {
      return null;
    }
  }
}
