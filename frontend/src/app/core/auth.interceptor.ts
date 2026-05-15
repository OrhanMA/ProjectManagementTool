import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthService).accessToken();
  const authenticatedRequest = token
    ? request.clone({
        withCredentials: true,
        setHeaders: { Authorization: `Bearer ${token}` }
      })
    : request.clone({ withCredentials: true });

  return next(authenticatedRequest);
};
