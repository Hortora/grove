import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

@customElement('grove-app')
class GroveApp extends LitElement {
  static styles = css`
    :host {
      display: block;
      height: 100%;
    }
    .shell {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    header {
      padding: 16px 24px;
      border-bottom: 1px solid #333;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #e0e0e0;
    }
    .subtitle {
      color: #888;
      font-size: 14px;
    }
    main {
      flex: 1;
      padding: 24px;
      overflow-y: auto;
    }
    nav a {
      color: #7cb3f5;
      text-decoration: none;
      margin-right: 16px;
      font-size: 14px;
    }
    nav a:hover { text-decoration: underline; }
    nav a.active { color: #e0e0e0; font-weight: 600; }
  `;

  @state() private route = '';

  connectedCallback() {
    super.connectedCallback();
    window.addEventListener('hashchange', () => this.updateRoute());
    this.updateRoute();
  }

  private updateRoute() {
    this.route = location.hash.replace('#', '') || 'home';
  }

  render() {
    return html`
      <div class="shell">
        <header>
          <h1>Grove</h1>
          <span class="subtitle">Garden Analytics & Curation</span>
        </header>
        <main>
          ${this.renderView()}
        </main>
      </div>
    `;
  }

  private renderView() {
    const route = this.route;
    if (route === 'home' || route === '') {
      return html`<p>Domain map — coming soon</p>`;
    }
    if (route.startsWith('domain/')) {
      const domain = route.substring(7);
      return html`<p>Domain detail: ${domain} — coming soon</p>`;
    }
    if (route.startsWith('entry/')) {
      const geId = route.substring(6);
      return html`<p>Entry detail: ${geId} — coming soon</p>`;
    }
    return html`<p>Unknown route: ${route}</p>`;
  }
}

const container = document.getElementById('app');
if (container) {
  container.appendChild(document.createElement('grove-app'));
}
