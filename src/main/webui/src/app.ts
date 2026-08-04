import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import './views/domain-map';
import './views/domain-detail';
import './views/entry-detail';
import './views/duplicate-review';
import './views/outlier-review';
import './views/cross-domain-review';
import './views/coverage-review';

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
    h1 a { color: inherit; text-decoration: none; }
    .subtitle {
      color: #888;
      font-size: 14px;
    }
    main {
      flex: 1;
      padding: 24px;
      overflow-y: auto;
    }
  `;

  @state() private route = '';
  @state() private routeParam = '';

  connectedCallback() {
    super.connectedCallback();
    window.addEventListener('hashchange', () => this.updateRoute());
    this.updateRoute();
  }

  private updateRoute() {
    const hash = location.hash.replace('#', '') || 'home';
    if (hash.startsWith('duplicates/')) {
      this.route = 'duplicates';
      this.routeParam = hash.substring(11);
    } else if (hash.startsWith('outliers/')) {
      this.route = 'outliers';
      this.routeParam = hash.substring(9);
    } else if (hash === 'cross-domain') {
      this.route = 'cross-domain';
      this.routeParam = '';
    } else if (hash.startsWith('coverage/')) {
      this.route = 'coverage';
      this.routeParam = hash.substring(9);
    } else if (hash.startsWith('domain/')) {
      this.route = 'domain';
      this.routeParam = hash.substring(7);
    } else if (hash.startsWith('entry/')) {
      this.route = 'entry';
      this.routeParam = hash.substring(6);
    } else {
      this.route = 'home';
      this.routeParam = '';
    }
  }

  render() {
    return html`
      <div class="shell">
        <header>
          <h1><a href="#">Grove</a></h1>
          <span class="subtitle">Garden Analytics &amp; Curation</span>
        </header>
        <main>
          ${this.renderView()}
        </main>
      </div>
    `;
  }

  private renderView() {
    switch (this.route) {
      case 'home':
        return html`<grove-domain-map></grove-domain-map>`;
      case 'domain':
        return html`<grove-domain-detail .domain=${this.routeParam}></grove-domain-detail>`;
      case 'entry':
        return html`<grove-entry-detail .geId=${this.routeParam}></grove-entry-detail>`;
      case 'duplicates':
        return html`<grove-duplicate-review .domain=${this.routeParam}></grove-duplicate-review>`;
      case 'outliers':
        return html`<grove-outlier-review .domain=${this.routeParam}></grove-outlier-review>`;
      case 'cross-domain':
        return html`<grove-cross-domain-review></grove-cross-domain-review>`;
      case 'coverage':
        return html`<grove-coverage-review .domain=${this.routeParam}></grove-coverage-review>`;
      default:
        return html`<p>Unknown route: ${this.route}</p>`;
    }
  }
}

const container = document.getElementById('app');
if (container) {
  container.appendChild(document.createElement('grove-app'));
}
