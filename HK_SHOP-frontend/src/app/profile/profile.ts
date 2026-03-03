import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProfileService, Profile } from './profile.service';
import { PageShellComponent } from '../shared/page-shell/page-shell'; // <-- AJOUT

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PageShellComponent   // <-- AJOUT ICI
  ],
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
confirmPasswordCtrl: any;
newPasswordCtrl: any;
currentPasswordCtrl: any;
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
    this.profile.getMe().subscribe({
      next: (me) => {
        this.loading = false;
        this.me = me;
        this.profileForm.patchValue({
          username: me.username,
          email: me.email
        });
      },
      error: (e: Error) => {
        this.loading = false;
        this.error = e.message;
      }
    });
  }

  saveProfile() {
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
      error: (e: Error) => {
        this.saving = false;
        this.error = e.message;
      }
    });
  }

  changePassword() {
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
      error: (e: Error) => {
        this.saving = false;
        this.error = e.message;
      }
    });
  }
}