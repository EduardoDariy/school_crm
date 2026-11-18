import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/toast.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="wrap">
      <form class="card login" (ngSubmit)="submit()">
        <div class="logo">S</div>
        <h1>Вход в School CRM</h1>
        <p class="muted">Для сотрудников школы</p>

        <label class="field"><span>Школа (поддомен)</span>
          <input class="input" name="subdomain" [(ngModel)]="subdomain" required autocomplete="organization" placeholder="например, demo" />
        </label>
        <label class="field"><span>Email</span>
          <input class="input" name="email" type="email" [(ngModel)]="email" required autocomplete="username" />
        </label>
        <label class="field"><span>Пароль</span>
          <input class="input" name="password" type="password" [(ngModel)]="password" required autocomplete="current-password" />
        </label>

        @if (error) { <div class="error-text">{{ error }}</div> }

        <button class="btn btn-primary" type="submit" [disabled]="loading || !subdomain || !email || !password">
          {{ loading ? 'Вход…' : 'Войти' }}
        </button>
      </form>
    </div>
  `,
  styles: `
    .wrap { min-height: 100vh; display: grid; place-items: center; padding: 24px;
      background: linear-gradient(160deg, #eef1fb 0%, var(--bg) 60%); }
    .login { width: 380px; max-width: 100%; display: flex; flex-direction: column; gap: 14px; padding: 32px; }
    .logo { width: 44px; height: 44px; border-radius: 10px; background: var(--accent); color: #fff;
      display: grid; place-items: center; font-weight: 700; font-size: 20px; }
    p { margin: -8px 0 4px; }
    button { height: 40px; margin-top: 4px; }
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  subdomain = this.auth.defaultSubdomain();
  email = '';
  password = '';
  loading = false;
  error = '';

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.login(this.subdomain, this.email, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: e => {
        this.loading = false;
        this.error = errorMessage(e);
      },
    });
  }
}
