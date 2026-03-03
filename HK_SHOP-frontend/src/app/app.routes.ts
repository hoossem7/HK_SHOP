import { Routes } from '@angular/router';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { ProfileComponent } from './profile/profile';
import { AuthGuard } from './auth/auth.guard';
import { SiteLayoutComponent } from './shared/site-layout/site-layout';

export const routes: Routes = [

  // 🔓 Public pages (no layout)
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // 🔒 Protected pages (with layout)
  {
    path: '',
    component: SiteLayoutComponent,
    canActivate: [AuthGuard], // protège tout le layout
    children: [
      { path: 'profile', component: ProfileComponent },

      // tu peux ajouter ici d'autres pages privées
      // { path: 'orders', component: OrdersComponent },

      { path: '', redirectTo: 'profile', pathMatch: 'full' }
    ]
  },

  { path: '**', redirectTo: 'profile' }
];