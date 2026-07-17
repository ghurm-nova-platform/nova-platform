import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Application root. Shell and feature routes render through the router.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
  styles: [
    `
      :host {
        display: block;
        height: 100%;
      }
    `,
  ],
})
export class App {}
