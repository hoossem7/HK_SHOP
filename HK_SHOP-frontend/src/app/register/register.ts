import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../auth/auth.service';
import { PageShellComponent } from '../shared/page-shell/page-shell';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageShellComponent],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class RegisterComponent implements OnInit {
  form!: FormGroup;
  activationForm!: FormGroup;

  step: 'register' | 'activate' = 'register';
  registeredEmail: string = '';

  loading = false;
  error: string | null = null;
  success: string | null = null;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });

    this.activationForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });
  }

  get f() { return this.form.controls; }
  get a() { return this.activationForm.controls; }

  submit(): void {
    this.error = null;
    this.success = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    const payload = {
      username: String(this.f['username'].value || '').trim(),
      email: String(this.f['email'].value || '').trim(),
      password: String(this.f['password'].value || ''),
    };

    this.registeredEmail = payload.email;

    this.auth.register(payload).subscribe({
      next: (msg: string) => {
        this.loading = false;
        this.success = msg || 'Inscription réussie. Un code a été envoyé par email.';
        this.step = 'activate';
        this.activationForm.reset();
      },
      error: (err: unknown) => {
        this.loading = false;
        this.error = this.extractError(err);
      }
    });
  }

  activate(): void {
    this.error = null;
    this.success = null;

    if (this.activationForm.invalid) {
      this.activationForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const payload = {
      email: this.registeredEmail,
      code: String(this.a['code'].value || '').trim(),
    };

    this.auth.activate(payload).subscribe({
      next: (msg: string) => {
        this.loading = false;
        this.success = msg || 'Compte activé. Vous pouvez vous connecter.';
        setTimeout(() => this.router.navigate(['/login']), 700);
      },
      error: (err: unknown) => {
        this.loading = false;
        this.error = this.extractError(err);
      }
    });
  }

  backToRegister(): void {
    this.step = 'register';
    this.error = null;
    this.success = null;
  }

  private extractError(err: unknown): string {

  if (!(err instanceof HttpErrorResponse)) {
    return 'Erreur inconnue.';
  }

  // backend renvoie parfois JSON sous forme de string
  if (typeof err.error === 'string' && err.error.trim().length > 0) {
    try {
      const parsed = JSON.parse(err.error);
      return parsed.message || parsed.businessErrorDescription || err.error;
    } catch {
      return err.error;
    }
  }

  const e: any = err.error;

  if (!e) {
    return err.message || 'Erreur serveur.';
  }

  switch (e.businessErrorCode) {
    case 305: return "Ce nom d'utilisateur est déjà utilisé.";
    case 306: return "Cet email est déjà utilisé. Essayez de vous connecter.";
    case 303: return "Votre compte n'est pas activé.";
    case 308: return "Mot de passe actuel incorrect.";
    case 309: return "Le nouveau mot de passe ne correspond pas.";
    case 310: return "Code d'activation invalide.";
    case 311: return "Code d'activation expiré.";
    case 312: return "Code déjà utilisé.";
    case 307: return "Utilisateur introuvable.";
    default:
      return e.message || e.businessErrorDescription || err.message || 'Une erreur est survenue.';
  }
}
}