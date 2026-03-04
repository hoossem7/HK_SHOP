import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../auth/auth.service';
import { TokenStorageService } from '../auth/token-storage.service';
import { PageShellComponent } from '../shared/page-shell/page-shell';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageShellComponent],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent implements OnInit {
  form!: FormGroup;

  loading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private tokenStorage: TokenStorageService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  get f() {
    return this.form.controls;
  }

  submit(): void {
    this.error = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.auth.login(this.form.value).subscribe({
      next: (res) => {
        this.loading = false;

        const token = res?.accessToken;
        if (!token) {
          this.error = "Réponse invalide du serveur (accessToken manquant).";
          return;
        }

        this.tokenStorage.saveToken(token);
        this.router.navigate(['/profile']);
      },
      error: (err: unknown) => {
        this.loading = false;
        this.error = this.extractError(err);
      }
    });
  }

  private extractError(err: unknown): string {
    // ✅ Cas principal : HttpErrorResponse (ce que ton service renvoie)
    if (err instanceof HttpErrorResponse) {
      const e: any = err.error;

      // 1) backend renvoie texte brut
      if (typeof e === 'string' && e.trim().length > 0) {
        return e;
      }

      // 2) backend renvoie ExceptionResponse JSON
      return (
        e?.message ||
        e?.businessErrorDescription ||
        err.message ||
        'Login failed'
      );
    }

    // ✅ Cas : Error classique
    if (err instanceof Error) {
      return err.message || 'Login failed';
    }

    // ✅ Cas : string
    if (typeof err === 'string') {
      return err || 'Login failed';
    }

    return 'Login failed';
  }
}