import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../auth/auth.service';
import { TokenStorageService } from '../../auth/token-storage.service';

@Component({
  selector: 'app-site-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive ],
  templateUrl: './site-layout.html',
  styleUrls: ['./site-layout.scss']
})
export class SiteLayoutComponent {
  currentYear: number = new Date().getFullYear();

  loadingLogout = false;

  // ✅ Confirmation modal state
  showLogoutConfirm = false;

  // ✅ Logo config
  logoSrc = 'assets/brand/hkshop-logo.png'; // mets ton logo ici
  showLogo = true; // si pas de logo, mets false => dot visible

  constructor(
    private auth: AuthService,
    private tokenStorage: TokenStorageService,
    private router: Router
  ) {}

  get isLoggedIn(): boolean {
    return this.tokenStorage.isLoggedIn();
  }

  get username(): string | null {
    return this.tokenStorage.getUsername();
  }

  // ===== Logout confirm flow =====

  openLogoutConfirm(): void {
    this.showLogoutConfirm = true;
  }

  closeLogoutConfirm(): void {
    if (this.loadingLogout) return;
    this.showLogoutConfirm = false;
  }

  confirmLogout(): void {
    if (this.loadingLogout) return;

    this.loadingLogout = true;

    this.auth.logout().subscribe({
      next: () => this.finishLogout(),
      error: () => {
        // Même si backend échoue, on logout côté front
        this.finishLogout();
      }
    });
  }

  private finishLogout(): void {
    this.loadingLogout = false;
    this.showLogoutConfirm = false;
    this.tokenStorage.clear();
    this.router.navigate(['/login']);
  }

  // fallback si l'image n'existe pas
  onLogoError(): void {
    this.showLogo = false;
  }
}