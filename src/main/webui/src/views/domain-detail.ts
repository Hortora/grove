import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import '../components/entry-table';
import '../components/health-bar';
import { Segment } from '../components/health-bar';

const TYPE_COLORS: Record<string, string> = {
  gotcha: '#e06c60',
  technique: '#6ca0dc',
  undocumented: '#d4a843',
  convention: '#6abf69',
  architectural: '#b07cd8',
  breaking: '#e04040',
};

@customElement('grove-domain-detail')
export class DomainDetail extends LitElement {
  static styles = css`
    :host { display: block; }
    .back {
      color: #7cb3f5;
      text-decoration: none;
      font-size: 13px;
      display: inline-block;
      margin-bottom: 16px;
    }
    .back:hover { text-decoration: underline; }
    .header {
      display: flex;
      align-items: baseline;
      gap: 16px;
      margin-bottom: 8px;
    }
    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #e0e0e0;
    }
    .summary {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;
      font-size: 13px;
      color: #999;
    }
    .summary span { color: #bbb; }
    .bar-section {
      max-width: 400px;
      margin-bottom: 20px;
    }
    .loading { color: #888; padding: 40px; text-align: center; }
    .error { color: #e06c60; padding: 20px; background: #2a1a1a; border-radius: 8px; }
  `;

  @property() domain = '';
  @state() private entries: any[] = [];
  @state() private loading = true;
  @state() private error = '';

  updated(changed: Map<string, unknown>) {
    if (changed.has('domain') && this.domain) {
      this.loadEntries();
    }
  }

  private async loadEntries() {
    this.loading = true;
    this.error = '';
    try {
      const res = await fetch(`/api/domains/${encodeURIComponent(this.domain)}/entries`);
      if (!res.ok) throw new Error(`API error: ${res.status}`);
      this.entries = await res.json();
    } catch (e) {
      this.error = `Failed to load entries: ${e}`;
    } finally {
      this.loading = false;
    }
  }

  private get typeSegments(): Segment[] {
    const counts: Record<string, number> = {};
    this.entries.forEach(e => {
      if (e.type) counts[e.type] = (counts[e.type] || 0) + 1;
    });
    return Object.entries(counts).map(([label, count]) => ({
      label,
      count,
      color: TYPE_COLORS[label] || '#888',
    }));
  }

  render() {
    if (this.loading) return html`<div class="loading">Loading ${this.domain} entries...</div>`;
    if (this.error) return html`<div class="error">${this.error}</div>`;

    const staleCount = this.entries.filter(e => e.stalenessStatus === 'stale').length;
    const avgScore = this.entries.length > 0
      ? (this.entries.reduce((sum, e) => sum + (e.score ?? 0), 0) / this.entries.length).toFixed(1)
      : '—';

    return html`
      <a class="back" href="#">&larr; Back to domain map</a>
      <div class="header">
        <h2>${this.domain}</h2>
      </div>
      <div class="summary">
        <div>${this.entries.length} entries</div>
        <div>Avg score: <span>${avgScore}</span></div>
        <div>Stale: <span>${staleCount}</span></div>
      </div>
      <div class="bar-section">
        <grove-health-bar .segments=${this.typeSegments} .showLegend=${true}></grove-health-bar>
      </div>
      <grove-entry-table .entries=${this.entries}></grove-entry-table>
    `;
  }
}
