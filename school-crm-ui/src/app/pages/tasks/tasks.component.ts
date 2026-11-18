import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService, TaskFilter } from '../../core/api.service';
import { Task } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { ToastService } from '../../core/toast.service';
import { TaskDialogComponent } from '../../shared/task-dialog.component';
import { TaskListComponent } from '../../shared/task-list.component';

type Scope = 'mine' | 'all' | 'done';

@Component({
  selector: 'app-tasks',
  imports: [TaskListComponent, TaskDialogComponent],
  template: `
    <div class="page narrow">
      <div class="page-header">
        <h1>Задачи</h1>
        <span class="spacer"></span>
        <button class="btn btn-primary" (click)="dialog.set(true)">+ Задача</button>
      </div>
      <div class="tabs">
        <button [class.active]="scope() === 'mine'" (click)="setScope('mine')">Мои открытые</button>
        <button [class.active]="scope() === 'all'" (click)="setScope('all')">Все сотрудники</button>
        <button [class.active]="scope() === 'done'" (click)="setScope('done')">Мои выполненные</button>
      </div>
      <div class="card">
        <app-task-list [tasks]="tasks()" [showAssignee]="scope() === 'all'" (changed)="load()"
                       [emptyText]="scope() === 'done' ? 'Выполненных задач пока нет' : 'Открытых задач нет'" />
      </div>
    </div>
    @if (dialog()) {
      <app-task-dialog (closed)="dialog.set(false)" (created)="dialog.set(false); load()" />
    }
  `,
  styles: `.narrow { max-width: 900px; } .spacer { flex: 1; }`,
})
export class TasksComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly staff = inject(StaffDirectoryService);

  protected readonly tasks = signal<Task[]>([]);
  protected readonly scope = signal<Scope>('mine');
  protected readonly dialog = signal(false);

  ngOnInit(): void {
    this.staff.load();
    this.load();
  }

  setScope(scope: Scope): void {
    this.scope.set(scope);
    this.load();
  }

  load(): void {
    const filters: Record<Scope, TaskFilter> = {
      mine: {},
      all: { all: true },
      done: { status: 'DONE' },
    };
    this.api.tasks(filters[this.scope()]).subscribe({
      next: tasks => this.tasks.set(this.scope() === 'done' ? tasks.reverse() : tasks),
      error: e => this.toast.error(e),
    });
  }
}
