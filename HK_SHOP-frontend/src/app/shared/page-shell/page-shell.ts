import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-page-shell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './page-shell.html',
  styleUrls: ['./page-shell.scss'], // peut rester vide
})
export class PageShellComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() showBrand = true;

  // normal = auth pages, medium = pages classiques, wide = dashboard/profil
  @Input() variant: 'normal' | 'medium' | 'wide' = 'normal';
}