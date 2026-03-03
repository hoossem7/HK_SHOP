import { Injectable } from '@angular/core';
import { User } from '../shared/models/user.model';

const TOKEN_KEY = 'hkshop-token';
const USER_KEY = 'hkshop-user';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {

  // ✅ Compat avec ton code existant
  signOut(): void {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(USER_KEY);
  }

  // ✅ Alias plus “standard”
  clear(): void {
    this.signOut();
  }

  saveToken(token: string): void {
    window.localStorage.setItem(TOKEN_KEY, token);
  }

  getToken(): string | null {
    return window.localStorage.getItem(TOKEN_KEY);
  }

  saveUser(user: User): void {
    window.localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  getUser(): User | null {
    const user = window.localStorage.getItem(USER_KEY);
    return user ? (JSON.parse(user) as User) : null;
  }

  // ✅ Helpers navbar/guards
  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getUsername(): string | null {
    return this.getUser()?.username ?? null;
  }
}