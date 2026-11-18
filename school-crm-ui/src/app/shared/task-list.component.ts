import { DatePipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Task } from '../core/models';
import { StaffDirectoryService } from '../core/staff-directory.service';
import { ToastService } from '../core/toast.service';

/** Список задач с отметкой о выполнении. */
@Component({
  selector: 'app-task-list',
  imports: [DatePipe, RouterLink],
  template: `
    @if (tasks().length === 0) {
      <div class="empty">{{ emptyText() }}</div>
    }
    <ul class="tasks">
      @for (task of tasks(); track task.id) {
        <li [class.done]="task.status !== 'OPEN'">
          <input type="checkbox" [checked]="task.status === 'DONE'" [disabled]="task.status === 'CANCELLED'"
                 (change)="toggle(task)" [attr.aria-label]="'Выполнено: ' + task.title" />
          <div class="content">
            <div class="title">{{ task.title }}</div>
            @if (task.description) { <div class="muted small">{{ task.description }}</div> }
            <div class="meta small">
              <span [class.overdue]="task.overdue">{{ task.overdue ? 'Просрочено · ' : '' }}{{ task.dueAt | date: 'd MMM, HH:mm' }}</span>
              @if (showAssignee()) { <span class="muted">· {{ staff.name(task.assigneeId) }}</span> }
              @if (showLead() && task.leadId) { <a [routerLink]="['/leads', task.leadId]">· лид</a> }
            </div>
          </div>
        </li>
      }
    </ul>
  `,
  styles: `
    .tasks { list-style: none; margin: 0; padding: 0; }
    li { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--border); }
    li:last-child { border-bottom: none; }
    input { margin-top: 3px; width: 16px; height: 16px; cursor: pointer; }
    .content { flex: 1; min-width: 0; }
    .title { font-weight: 500; }
    li.done .title { text-decoration: line-through; color: var(--text-muted); }
    .meta { display: flex; gap: 4px; margin-top: 2px; color: var(--text-muted); flex-wrap: wrap; }
    .overdue { color: var(--danger); font-weight: 500; }
  `,
})
export class TaskListComponent {
  readonly tasks = input.required<Task[]>();
  readonly showAssignee = input(false);
  readonly showLead = input(true);
  readonly emptyText = input('Задач нет');
  readonly changed = output<Task>();

  protected readonly staff = inject(StaffDirectoryService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  toggle(task: Task): void {
    const action = task.status === 'DONE' ? 'reopen' : 'complete';
    this.api.setTaskStatus(task.id, action).subscribe({
      next: updated => this.changed.emit(updated),
      error: e => this.toast.error(e),
    });
  }
}
