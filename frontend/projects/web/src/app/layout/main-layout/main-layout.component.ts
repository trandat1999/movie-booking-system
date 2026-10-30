import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthStore } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';

interface NavItem {
  label: string;
  link: string;
}

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, NzLayoutModule, NzMenuModule, NzButtonModule, NzIconModule, NzDropDownModule],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.less',
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly year = new Date().getFullYear();

  protected readonly navItems: NavItem[] = [
    { label: 'Trang chủ', link: '/' },
    { label: 'Phim', link: '/movies' },
    { label: 'Rạp chiếu', link: '/cinemas' },
    { label: 'Đặt vé nhóm', link: '/groups' },
  ];

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/'));
  }
}
