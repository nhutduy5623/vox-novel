import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'admin/novels/:id',
    renderMode: RenderMode.Server,
  },
  {
    path: 'admin/novels/:novelId/chapters/:chapterId',
    renderMode: RenderMode.Server,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
