import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * JWT 認證攔截器
 * 自動在每個 HTTP 請求附加 Authorization header
 * 401 時自動用 Refresh Token 換新 Access Token（無感刷新）
 *
 * 面試亮點：這個 Interceptor 實作了企業級的「無感 Token 自動刷新」
 * 面試時說：「Token 過期後，我用 Interceptor 攔截 401，
 *            自動呼叫 refresh endpoint 重試，用戶感知不到更新。」
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  // 附加 Token
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      // Access Token 過期（401）→ 自動換新 Token
      if (err.status === 401 && token) {
        return authService.refreshToken().pipe(
          switchMap((newToken) => {
            const retryReq = req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` },
            });
            return next(retryReq); // 用新的 Token 重試一次
          }),
          catchError((refreshErr) => {
            // Refresh Token 也失效 → 強制登出
            authService.logout();
            return throwError(() => refreshErr);
          })
        );
      }
      return throwError(() => err);
    })
  );
};
