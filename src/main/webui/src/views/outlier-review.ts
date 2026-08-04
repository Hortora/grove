import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('grove-outlier-review')
export class OutlierReview extends LitElement {
  static styles = css`
    :host { display: block; }
    .back { color: #7cb3f5; text-decoration: none; font-size: 13px; display: inline-block; margin-bottom: 16px; }
    .back:hover { text-decoration: underline; }
    h2 { margin: 0 0 16px; font-size: 20px; font-weight: 600; color: #e0e0e0; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th { text-align: left; padding: 8px 12px; border-bottom: 2px solid #3a3a3a; color: #999; font-weight: 500; }
    td { padding: 8px 12px; border-bottom: 1px solid #2a2a2a; color: #ccc; }
    tr:hover td { background: #2a2a2a; }
    .ge-link { color: #7cb3f5; text-decoration: none; font-family: monospace; font-size: 12px; }
    .ge-link:hover { text-decoration: underline; }
    .distance { font-weight: 600; }
    .high { color: #e06c60; }
    .medium { color: #d4a843; }
    .low { color: #6abf69; }
    .loading { color: #888; text-align: center; padding: 40px; }
    .empty { color: #666; text-align: center; padding: 40px; }
  `;

  @property() domain = '';
  @state() private entries: any[] = [];
  @state() private loading = true;

  connectedCallback() { super.connectedCallback(); if (this.domain) this.load(); }
  updated(changed: Map<string, unknown>) { if (changed.has('domain') && this.domain) this.load(); }

  private async load() {
    this.loading = true;
    try {
      const res = await fetch(`/api/analysis/outliers/${encodeURIComponent(this.domain)}`);
      if (res.ok) { const data = await res.json(); this.entries = data.entries || []; }
    } catch (_) { /* ignore */ }
    this.loading = false;
  }

  private extractGeId(s: string): string { return s?.match(/(GE-[^.]+)/)?.[1] || s || ''; }

  private distClass(d: number): string { return d > 0.5 ? 'high' : d > 0.3 ? 'medium' : 'low'; }

  render() {
    if (this.loading) return html`<div class="loading">Loading outlier analysis...</div>`;
    return html`
      <a class="back" href="#domain/${this.domain}">&larr; Back to ${this.domain}</a>
      <h2>Semantic Outliers — ${this.domain}</h2>
      ${this.entries.length === 0 ? html`<div class="empty">No entries found.</div>` : html`
        <table>
          <thead><tr><th>GE-ID</th><th>Title</th><th>Distance from Centroid</th></tr></thead>
          <tbody>
            ${this.entries.map(e => html`
              <tr>
                <td><a class="ge-link" href="#entry/${this.extractGeId(e.sourceDocumentId)}">${this.extractGeId(e.sourceDocumentId)}</a></td>
                <td>${e.title}</td>
                <td><span class="distance ${this.distClass(e.distanceFromCentroid)}">${e.distanceFromCentroid.toFixed(4)}</span></td>
              </tr>
            `)}
          </tbody>
        </table>
      `}
    `;
  }
}
