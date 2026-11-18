import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ROLE_LABELS } from '../core/labels';
import { StaffRole } from '../core/models';
import { ToastService } from '../core/toast.service';
import { ModalComponent } from '../shared/modal.component';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  roles: StaffRole[];
  exact?: boolean;
}

const NAV: NavItem[] = [
  { path: '/', label: 'Главная', icon: '⌂', roles: ['ADMIN', 'MANAGER', 'TEACHER', 'FINANCE'], exact: true },
  { path: '/leads', label: 'Лиды', icon: '◎', roles: ['ADMIN', 'MANAGER'] },
  { path: '/tasks', label: 'Задачи', icon: '✓', roles: ['ADMIN', 'MANAGER'] },
  { path: '/groups', label: 'Группы', icon: '▦', roles: ['ADMIN', 'MANAGER', 'TEACHER'] },
  { path: '/staff', label: 'Сотрудники', icon: '☺', roles: ['ADMIN'] },
  { path: '/settings/pipeline', label: 'Настройки воронки', icon: '⚙', roles: ['ADMIN'] },
];

/** Общий макет: боковое меню по ролям, сотрудник и выход. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule, ModalComponent],
  template: `
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">{{ orgName().charAt(0) || 'S' }}</div>
        <div class="brand-name" [title]="orgName()">{{ orgName() || 'School CRM' }}</div>
      </div>
      <nav>
        @for (item of nav(); track item.path) {
          <a [routerLink]="item.path" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: !!item.exact }">
            <span class="icon" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}
          </a>
        }
      </nav>
      <div class="user">
        <div class="user-name">{{ auth.user()?.fullName }}</div>
        <div class="muted small">{{ roleLabel() }}</div>
        <div class="row user-actions">
          <button class="btn btn-link small" (click)="passwordOpen.set(true)">Сменить пароль</button>
          <button class="btn btn-link small" (click)="auth.logout()">Выйти</button>
        </div>
      </div>
    </aside>
    <main><router-outlet /></main>

    @if (passwordOpen()) {
      <app-modal title="Смена пароля" [width]="400" (closed)="passwordOpen.set(false)">
        <form id="pwd-form" class="stack" (ngSubmit)="changePassword()">
          <label class="field"><span>Текущий пароль</span>
            <input class="input" type="password" name="current" [(ngModel)]="currentPassword" autocomplete="current-password" required />
          </label>
          <label class="field"><span>Новый пароль (не короче 8 символов)</span>
            <input class="input" type="password" name="new" [(ngModel)]="newPassword" autocomplete="new-password" required minlength="8" />
          </label>
        </form>
        <ng-container actions>
          <button class="btn" type="button" (click)="passwordOpen.set(false)">Отмена</button>
          <button class="btn btn-primary" type="submit" form="pwd-form" [disabled]="newPassword.length < 8 || !currentPassword">Сохранить</button>
        </ng-container>
      </app-modal>
    }
  `,
  styles: `
    :host { display: flex; min-height: 100vh; }
    .sidebar { width: var(--sidebar-width); flex-shrink: 0; background: #151a26; color: #c9cfdb;
      display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; }
    .brand { display: flex; align-items: center; gap: 10px; padding: 18px 16px; }
    .logo { width: 32px; height: 32px; border-radius: 8px; background: var(--accent); color: #fff;
      display: grid; place-items: center; font-weight: 700; flex-shrink: 0; }
    .brand-name { color: #fff; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    nav { flex: 1; padding: 4px 8px; display: flex; flex-direction: column; gap: 2px; }
    nav a { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border-radius: 6px; color: #c9cfdb; }
    nav a:hover { background: rgba(255, 255, 255, .06); text-decoration: none; }
    nav a.active { background: rgba(255, 255, 255, .1); color: #fff; font-weight: 500; }
    .icon { width: 18px; text-align: center; opacity: .85; }
    .user { padding: 14px 16px; border-top: 1px solid rgba(255, 255, 255, .08); }
    .user-name { color: #fff; font-weight: 500; }
    .user .muted { color: #8b93a7; }
    .user-actions { gap: 14px; margin-top: 6px; }
    .user-actions .btn-link { color: #9db0ff; font-size: 12px; }
    main { flex: 1; min-width: 0; }
  `,
})
export class ShellComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  protected readonly orgName = signal('');
  protected readonly passwordOpen = signal(false);
  protected readonly nav = computed(() => {
    const role = this.auth.user()?.role;
    return NAV.filter(item => role && item.roles.includes(role));
  });
  protected readonly roleLabel = computed(() => {
    const role = this.auth.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });

  currentPassword = '';
  newPassword = '';

  ngOnInit(): void {
    this.auth.refreshUser();
    this.api.organization().subscribe({ next: org => this.orgName.set(org.name) });
  }

  changePassword(): void {
    this.auth.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.toast.success('Пароль изменён');
        this.passwordOpen.set(false);
        this.currentPassword = '';
        this.newPassword = '';
      },
      error: e => this.toast.error(e),
    });
  }
}
