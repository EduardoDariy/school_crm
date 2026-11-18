import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Problem } from './models';

export interface Toast {
  id: number;
  kind: 'success' | 'error';
  text: string;
}

/** Всплывающие уведомления об успехе и ошибках. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  success(text: string): void {
    this.push('success', text);
  }

  error(error: unknown): void {
    this.push('error', errorMessage(error));
  }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private push(kind: Toast['kind'], text: string): void {
    const id = this.nextId++;
    this.toasts.update(list => [...list, { id, kind, text }]);
    setTimeout(() => this.dismiss(id), kind === 'error' ? 6000 : 3000);
  }
}

/** Текст ошибки из Problem Details backend или общий текст. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as Problem | null;
    if (problem?.detail) {
      return problem.detail;
    }
    if (error.status === 0) {
      return 'Сервер недоступен. Проверьте подключение.';
    }
    if (error.status === 403) {
      return 'Недостаточно прав';
    }
    return `Ошибка ${error.status}`;
  }
  return 'Неизвестная ошибка';
}

/** id существующей записи из ответа 409 о дубле. */
export function duplicateId(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && error.status === 409) {
    return (error.error as Problem | null)?.existingId ?? null;
  }
  return null;
}
