import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  
}

export interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
}

export interface ActivatePayload {
  email: string;
  code: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  // ✅ responseType text, et ON NE TOUCHE PAS aux erreurs (HttpErrorResponse arrive au component)
  register(payload: RegisterPayload): Observable<string> {
    return this.http.post(`${this.API}/register`, payload, {
      responseType: 'text'
    });
  }

  // ✅ responseType text, idem
  activate(payload: ActivatePayload): Observable<string> {
    return this.http.post(`${this.API}/activate`, payload, {
      responseType: 'text'
    });
  }

  // ✅ login retourne JSON
  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, payload, {
      withCredentials: true
    });
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/refresh`, {}, {
      withCredentials: true
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.API}/logout`, {}, {
      withCredentials: true
    });
  }
}