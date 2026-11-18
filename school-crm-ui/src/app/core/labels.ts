import { LeadEventType, LeadStatus, StaffRole, TaskStatus } from './models';

/** Русские подписи для значений API. */

export const ROLE_LABELS: Record<StaffRole, string> = {
  ADMIN: 'Администратор',
  MANAGER: 'Менеджер',
  TEACHER: 'Преподаватель',
  FINANCE: 'Финансист',
};

export const ROLES: StaffRole[] = ['ADMIN', 'MANAGER', 'TEACHER', 'FINANCE'];

export const LEAD_STATUS_LABELS: Record<LeadStatus, string> = {
  OPEN: 'В работе',
  WON: 'Успешно',
  LOST: 'Проигран',
};

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  OPEN: 'Открыта',
  DONE: 'Выполнена',
  CANCELLED: 'Отменена',
};

export const LEAD_EVENT_LABELS: Record<LeadEventType, string> = {
  CREATED: 'Лид создан',
  UPDATED: 'Изменены контакты',
  STAGE_CHANGED: 'Этап изменён',
  MANAGER_CHANGED: 'Сменился ответственный',
  WON: 'Успешная продажа',
  LOST: 'Лид проигран',
  REOPENED: 'Возвращён в работу',
  ARCHIVED: 'Перемещён в архив',
  UNARCHIVED: 'Возвращён из архива',
};

export const LOGIN_FAILURE_LABELS: Record<string, string> = {
  UNKNOWN_ORGANIZATION: 'Неизвестная организация',
  ORGANIZATION_BLOCKED: 'Организация заблокирована',
  UNKNOWN_USER: 'Неизвестный email',
  BAD_PASSWORD: 'Неверный пароль',
  USER_BLOCKED: 'Учётная запись заблокирована',
};

export const LEAD_SOURCES = ['Instagram', 'WhatsApp', 'Telegram', 'Звонок', 'Сайт', 'Рекомендация', 'Другое'];
