import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfileService, Profile } from './profile.service';
import { PageShellComponent } from '../shared/page-shell/page-shell';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageShellComponent],
  templateUrl: './profile.html',
  styleUrls: ['./profile.scss']
})
export class ProfileComponent implements OnInit {

  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  loading = false;
  saving = false;
  error: string | null = null;
  success: string | null = null;

  me: Profile | null = null;
newPasswordCtrl: any;
currentPasswordCtrl: any;
confirmPasswordCtrl: any;
usernameCtrl: any;

  constructor(
    private fb: FormBuilder,
    private profile: ProfileService
  ) {}

  ngOnInit(): void {
    this.profileForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
    });

    this.load();
  }

  load() {
    this.loading = true;
    this.error = null;
    this.success = null;

    this.profile.getMe().subscribe({
      next: (me) => {
        this.loading = false;
        this.me = me;
        this.profileForm.patchValue({
          username: me.username,
          email: me.email
        });
      },
      error: (err: unknown) => {
        this.loading = false;
        this.error = this.extractError(err);
      }
    });
  }

  saveProfile() {
    this.error = null;
    this.success = null;

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.saving = true;

    this.profile.updateMe(this.profileForm.value).subscribe({
      next: (me) => {
        this.saving = false;
        this.me = me;
        this.success = "Profil mis à jour.";
      },
      error: (err: unknown) => {
        this.saving = false;
        this.error = this.extractError(err);
      }
    });
  }

  changePassword() {
    this.error = null;
    this.success = null;

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const value = this.passwordForm.value;

    if (value.newPassword !== value.confirmPassword) {
      this.error = "Les mots de passe ne correspondent pas.";
      return;
    }

    this.saving = true;

    this.profile.changePassword(value).subscribe({
      next: () => {
        this.saving = false;
        this.success = "Mot de passe changé avec succès.";
        this.passwordForm.reset();
      },
      error: (err: unknown) => {
        this.saving = false;
        this.error = this.extractError(err);
      }
    });
  }

  private extractError(err: unknown): string {
    // Si ton service/interceptor renvoie directement une string
    if (typeof err === 'string' && err.trim()) return err;

    if (!(err instanceof HttpErrorResponse)) {
      // Si ton service renvoie new Error(msg)
      if (err instanceof Error) return err.message || 'Erreur inconnue.';
      return 'Erreur inconnue.';
    }

    // backend renvoie parfois du JSON sous forme string
    if (typeof err.error === 'string' && err.error.trim().length > 0) {
      try {
        const parsed = JSON.parse(err.error);
        return parsed.message || parsed.businessErrorDescription || err.error;
      } catch {
        return err.error || err.message || 'Une erreur est survenue.';
      }
    }

    const e: any = err.error;
    return (
      e?.message ||
      e?.businessErrorDescription ||
      err.message ||
      'Une erreur est survenue.'
    );
  }
}