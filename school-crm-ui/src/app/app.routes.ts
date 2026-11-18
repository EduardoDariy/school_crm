import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
    title: 'Вход — School CRM',
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Главная — School CRM',
      },
      {
        path: 'leads',
        canActivate: [roleGuard('ADMIN', 'MANAGER')],
        loadComponent: () => import('./pages/leads/leads-board.component').then(m => m.LeadsBoardComponent),
        title: 'Лиды — School CRM',
      },
      {
        path: 'leads/:id',
        canActivate: [roleGuard('ADMIN', 'MANAGER')],
        loadComponent: () => import('./pages/leads/lead-detail.component').then(m => m.LeadDetailComponent),
        title: 'Лид — School CRM',
      },
      {
        path: 'tasks',
        canActivate: [roleGuard('ADMIN', 'MANAGER')],
        loadComponent: () => import('./pages/tasks/tasks.component').then(m => m.TasksComponent),
        title: 'Задачи — School CRM',
      },
      {
        path: 'groups',
        canActivate: [roleGuard('ADMIN', 'MANAGER', 'TEACHER')],
        loadComponent: () => import('./pages/groups/groups.component').then(m => m.GroupsComponent),
        title: 'Группы — School CRM',
      },
      {
        path: 'staff',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./pages/staff/staff.component').then(m => m.StaffComponent),
        title: 'Сотрудники — School CRM',
      },
      {
        path: 'settings/pipeline',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./pages/settings/pipeline-settings.component').then(m => m.PipelineSettingsComponent),
        title: 'Воронка — School CRM',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
