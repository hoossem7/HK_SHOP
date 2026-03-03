import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private tokenStorage: TokenStorageService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedRoles: string[] = route.data['roles'];
    const user = this.tokenStorage.getUser();
    if (!user || !user.roles) {
      this.router.navigate(['/login']);
      return false;
    }
    const hasRole = user.roles.some((r: string) => expectedRoles.includes(r));
    if (!hasRole) {
      this.router.navigate(['/forbidden']);
      return false;
    }
    return true;
  }
}
