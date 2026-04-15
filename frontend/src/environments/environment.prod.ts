// 正式環境設定（ng build --configuration=production 時使用）
export const environment = {
  production: true,
  apiBaseUrl: '/api',  // Nginx 反向代理，同 domain 下
};
