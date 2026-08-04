import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '../components/domain-card';

@customElement('grove-domain-map')
export class DomainMap extends LitElement {
  static styles = css`
    :host { display: block; }
    .overview {
      display: flex;
      gap: 24px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }
    .metric {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 8px;
      padding: 16px 20px;
      min-width: 140px;
    }
    .metric-value {
      font-size: 28px;
      font-weight: 700;
      color: #7cb3f5;
    }
    .metric-label {
      font-size: 12px;
      color: #888;
      margin-top: 4px;
    }
    .metric-warn .metric-value { color: #e06c60; }
    .metric-alert .metric-value { color: #d4a843; }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 16px;
    }
    .loading {
      color: #888;
      padding: 40px;
      text-align: center;
    }
    .error {
      color: #e06c60;
      padding: 20px;
      background: #2a1a1a;
      border-radius: 8px;
    }
    .actions {
      display: flex;
      gap: 12px;
      margin-bottom: 24px;
    }
    .reindex-btn {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 6px;
      color: #e0e0e0;
      padding: 8px 16px;
      cursor: pointer;
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: border-color 0.2s;
    }
    .reindex-btn:hover:not(:disabled) {
      border-color: #7cb3f5;
    }
    .reindex-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .reindex-result {
      font-size: 13px;
      padding: 8px 12px;
      border-radius: 6px;
      align-self: center;
    }
    .reindex-result.ok {
      color: #7cb3f5;
      background: #1a2a3a;
    }
    .reindex-result.error {
      color: #e06c60;
      background: #2a1a1a;
    }
  `;

  @state() private domains: any[] = [];
  @state() private overview: any = null;
  @state() private loading = true;
  @state() private error = '';
  @state() private reindexing = false;
  @state() private reindexResult: { status: string; message: string } | null = null;

  connectedCallback() {
    super.connectedCallback();
    this.loadData();
  }

  private async loadData() {
    try {
      const [domainsRes, overviewRes] = await Promise.all([
        fetch('/api/domains'),
        fetch('/api/overview'),
      ]);
      if (!domainsRes.ok || !overviewRes.ok) throw new Error('API error');
      this.domains = await domainsRes.json();
      this.overview = await overviewRes.json();
    } catch (e) {
      this.error = `Failed to load garden data: ${e}`;
    } finally {
      this.loading = false;
    }
  }

  private async triggerReindex() {
    this.reindexing = true;
    this.reindexResult = null;
    try {
      const res = await fetch('/api/reindex', { method: 'POST' });
      this.reindexResult = await res.json();
      if (this.reindexResult?.status === 'ok') {
        await this.loadData();
      }
    } catch (e) {
      this.reindexResult = { status: 'error', message: `Request failed: ${e}` };
    } finally {
      this.reindexing = false;
    }
  }

  render() {
    if (this.loading) return html`<div class="loading">Loading garden data...</div>`;
    if (this.error) return html`<div class="error">${this.error}</div>`;

    const o = this.overview;
    return html`
      <div class="overview">
        <div class="metric">
          <div class="metric-value">${o.totalEntries}</div>
          <div class="metric-label">Total entries</div>
        </div>
        <div class="metric">
          <div class="metric-value">${o.totalDomains}</div>
          <div class="metric-label">Domains</div>
        </div>
        <div class="metric ${o.staleCount > 0 ? 'metric-warn' : ''}">
          <div class="metric-value">${o.staleCount}</div>
          <div class="metric-label">Stale entries</div>
        </div>
        <div class="metric ${o.neverRetrievedCount > 0 ? 'metric-alert' : ''}">
          <div class="metric-value">${o.neverRetrievedCount}</div>
          <div class="metric-label">Never retrieved</div>
        </div>
      </div>
      <div class="actions">
        <button class="reindex-btn" @click=${this.triggerReindex} ?disabled=${this.reindexing}>
          ${this.reindexing ? 'Reindexing...' : 'Trigger Reindex'}
        </button>
        ${this.reindexResult ? html`
          <span class="reindex-result ${this.reindexResult.status === 'ok' ? 'ok' : 'error'}">
            ${this.reindexResult.message}
          </span>
        ` : ''}
      </div>
      <div class="grid">
        ${this.domains.map(d => html`
          <grove-domain-card .stats=${d}></grove-domain-card>
        `)}
      </div>
    `;
  }
}
