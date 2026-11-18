import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Group } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { ToastService } from '../../core/toast.service';

/** Учебные группы. Преподаватель видит только свои (фильтрует backend). */
@Component({
  selector: 'app-groups',
  template: `
    <div class="page narrow">
      <div class="page-header">
        <h1>{{ auth.hasRole('TEACHER') ? 'Мои группы' : 'Группы' }}</h1>
      </div>
      @if (groups().length === 0) {
        <div class="card empty">Групп пока нет</div>
      }
      <div class="grid-2">
        @for (group of groups(); track group.id) {
          <section class="card">
            <h2>{{ group.name }}</h2>
            @if (!auth.hasRole('TEACHER')) {
              <div class="muted small teacher">Преподаватель: {{ group.teacherId ? staff.name(group.teacherId) : 'не назначен' }}</div>
            }
            @if ((group.students ?? []).length === 0) {
              <div class="muted">Учеников нет</div>
            }
            <ul>
              @for (s of group.students ?? []; track s.id) {
                <li>{{ s.firstName }} {{ s.lastName ?? '' }} <span class="muted small">{{ s.phoneNumber ?? '' }}</span></li>
              }
            </ul>
          </section>
        }
      </div>
    </div>
  `,
  styles: `
    .narrow { max-width: 1100px; }
    .teacher { margin: -6px 0 10px; }
    ul { margin: 0; padding-left: 18px; }
    li { padding: 3px 0; }
  `,
})
export class GroupsComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly staff = inject(StaffDirectoryService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  protected readonly groups = signal<Group[]>([]);

  ngOnInit(): void {
    if (this.auth.hasRole('ADMIN', 'MANAGER')) {
      this.staff.load();
    }
    this.api.groups().subscribe({ next: g => this.groups.set(g), error: e => this.toast.error(e) });
  }
}
