import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Profile {
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface UpdateProfilePayload {
  username: string;
  email: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly API = 'http://localhost:8080/api/users/me';

  constructor(private http: HttpClient) {}

  getMe(): Observable<Profile> {
    return this.http.get<Profile>(this.API);
  }

  updateMe(payload: UpdateProfilePayload): Observable<Profile> {
    return this.http.put<Profile>(this.API, payload);
  }

  changePassword(payload: ChangePasswordPayload): Observable<void> {
    return this.http.put<void>(`${this.API}/password`, payload);
  }
}