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
  }

  get f() {
    return this.form.controls;
  }

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

    this.auth.register(payload).subscribe({
      next: (msg: string) => {
        this.loading = false;
        this.success = msg || 'Inscription réussie. Veuillez vous connecter.';
        setTimeout(() => this.router.navigate(['/login']), 900);
      },
      error: (err: unknown) => {
        this.loading = false;

        if (err instanceof HttpErrorResponse) {
          this.error =
            err.error?.businessErrorDescription ||
            err.error?.message ||
            err.error?.error ||
            err.message ||
            'Registration failed';
        } else {
          this.error = 'Registration failed';
        }
      }
    });
  }
}