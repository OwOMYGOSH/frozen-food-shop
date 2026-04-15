import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';

/**
 * 路由設定
 * - Lazy Loading：每個 feature 獨立打包，加速首頁載入
 * - Route Guard：authGuard 要求登入、adminGuard 要求 Admin 角色
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.component').then((m) => m.HomeComponent),
    title: '冷凍食品電商 - 首頁',
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./features/product/product-detail.component').then(
        (m) => m.ProductDetailComponent
      ),
    title: '商品詳情',
  },
  {
    path: 'cart',
    loadComponent: () =>
      import('./features/cart/cart.component').then((m) => m.CartComponent),
    title: '購物車',
  },
  {
    path: 'checkout',
    loadComponent: () =>
      import('./features/checkout/checkout.component').then(
        (m) => m.CheckoutComponent
      ),
    canActivate: [authGuard],
    title: '結帳',
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./features/orders/orders.component').then(
        (m) => m.OrdersComponent
      ),
    canActivate: [authGuard],
    title: '我的訂單',
  },
  {
    path: 'admin',
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.adminRoutes),
    canActivate: [adminGuard],
    title: '後台管理',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent
      ),
    title: '登入',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent
      ),
    title: '註冊',
  },
  { path: '**', redirectTo: '' },
];
