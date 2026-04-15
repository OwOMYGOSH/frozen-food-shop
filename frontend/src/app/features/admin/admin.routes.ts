import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
  {
    path: '',
    // Phase 4 實作：後台首頁 Dashboard
    loadComponent: () =>
      import('./dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
    title: '後台 - 總覽',
  },
  // Phase 4：商品管理、訂單管理、會員列表 路由在此擴充
];
