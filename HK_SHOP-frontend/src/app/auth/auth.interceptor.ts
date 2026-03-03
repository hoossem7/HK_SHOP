import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

const AUTH_API_PREFIX = '/api/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1) Toujours envoyer les cookies (refreshToken) vers ton backend
  //    IMPORTANT pour /login, /refresh, /logout (cookie HttpOnly)
  let request = req.clone({ withCredentials: true });

  // 2) Ne pas mettre Bearer sur endpoints auth
  const isAuthEndpoint = request.url.includes(AUTH_API_PREFIX);
  const token = tokenStorage.getToken();

  if (token && !isAuthEndpoint) {
    request = request.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(request).pipe(
    catchError((err: unknown) => {
      // Uniquement 401
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }

      // Si le 401 vient déjà de refresh/login/register/logout => pas de refresh loop
      if (isAuthEndpoint) {
        tokenStorage.signOut?.();
        router.navigate(['/login']);
        return throwError(() => err);
      }

      // 3) Essayer refresh (cookie) puis rejouer la requête originale
      return authService.refresh().pipe(
        switchMap(res => {
          const newToken = res?.accessToken;
          if (!newToken) {
            tokenStorage.signOut?.();
            router.navigate(['/login']);
            return throwError(() => err);
          }

          tokenStorage.saveToken(newToken);

          const retry = req.clone({
            withCredentials: true,
            setHeaders: { Authorization: `Bearer ${newToken}` }
          });
          return next(retry);
        }),
        catchError((refreshErr) => {
          tokenStorage.signOut?.();
          router.navigate(['/login']);
          return throwError(() => refreshErr);
        })
      );
    })
  );
};