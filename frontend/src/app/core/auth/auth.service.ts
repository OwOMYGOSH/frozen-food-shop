import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models';

interface LoginRequest {
  email: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  user: User;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'access_token';
  private readonly api = environment.apiBaseUrl;

  // Angular Signal：當前登入用戶（響應式狀態）
  currentUser = signal<User | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    // 初始化時從 localStorage 恢復登入狀態
    const token = this.getAccessToken();
    // 頁面重整後，user 資訊丟失，但 localStorage 仍有 token，所以重新 fetch
    if (token) {
      this.fetchCurrentUser().subscribe();
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, credentials).pipe(
      tap((res) => {
        localStorage.setItem(this.TOKEN_KEY, res.accessToken);
        this.currentUser.set(res.user); // 設定用戶狀態
      })
    );
  }

  logout(): void {
    const token = this.getAccessToken();
    if (token) {
      // 通知後端把 token 加入 Blacklist (fire and forget => 發出後就不管結果)
      // 相比為了等 API 回傳而增加登出流程的延遲，選擇接受 API 失敗的風險
      this.http.post(`${this.api}/auth/logout`, {}).subscribe();
    }
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  refreshToken(): Observable<string> {
    // Refresh Token 存在 HttpOnly Cookie，瀏覽器自動帶出
    return this.http.post<AuthResponse>(`${this.api}/auth/refresh`, {}).pipe(
      tap((res) => localStorage.setItem(this.TOKEN_KEY, res.accessToken)),
      map((res) => res.accessToken)
    );
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  private fetchCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.api}/users/me`).pipe(
      tap((user) => this.currentUser.set(user))
    );
  }
}
