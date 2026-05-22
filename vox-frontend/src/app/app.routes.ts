import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';

export const routes: Routes = [
  { path: '', redirectTo: 'admin/novels', pathMatch: 'full' },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    children: [
      { path: '', redirectTo: 'novels', pathMatch: 'full' },
      {
        path: 'novels',
        loadComponent: () =>
          import('./features/novels/novel-list/novel-list.component').then((m) => m.NovelListComponent),
      },
      {
        path: 'novels/:id',
        loadComponent: () =>
          import('./features/novels/novel-detail/novel-detail.component').then((m) => m.NovelDetailComponent),
      },
      {
        path: 'novels/:novelId/chapters/:chapterId',
        loadComponent: () =>
          import('./features/chapters/chapter-detail/chapter-detail.component').then(
            (m) => m.ChapterDetailComponent,
          ),
      },
    ],
  },
];
