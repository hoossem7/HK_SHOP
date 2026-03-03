import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  roles?: string[]; // optionnel
}

export interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  register(payload: RegisterPayload): Observable<string> {
    // ton backend renvoie: "User registered successfully" (string)
    return this.http
      .post(`${this.API}/register`, payload, { responseType: 'text' })
      .pipe(catchError(this.handleError));
  }

  login(payload: LoginPayload): Observable<AuthResponse> {
    // cookie refreshToken => withCredentials
    return this.http
      .post<AuthResponse>(`${this.API}/login`, payload, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  refresh(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API}/refresh`, {}, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.API}/logout`, {}, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  private handleError(err: HttpErrorResponse) {
    // Cas 1: backend renvoie une string (ex: "Email is already in use.")
    if (typeof err.error === 'string' && err.error.trim().length > 0) {
      return throwError(() => new Error(err.error));
    }

    // Cas 2: backend renvoie un JSON { message: ... }
    const msg =
      err.error?.message ||
      err.message ||
      'Une erreur est survenue.';

    return throwError(() => new Error(msg));
  }
}