import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('grove-coverage-review')
export class CoverageReview extends LitElement {
  static styles = css`
    :host { display: block; }
    .back { color: #7cb3f5; text-decoration: none; font-size: 13px; display: inline-block; margin-bottom: 16px; }
    .back:hover { text-decoration: underline; }
    h2 { margin: 0 0 8px; font-size: 20px; font-weight: 600; color: #e0e0e0; }
    .summary { display: flex; gap: 24px; margin-bottom: 20px; font-size: 13px; color: #999; }
    .summary span { color: #bbb; }
    .cluster {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 12px;
    }
    .cluster-header { font-size: 14px; font-weight: 600; color: #7cb3f5; margin-bottom: 8px; }
    .cluster-entries { display: flex; flex-wrap: wrap; gap: 8px; }
    .cluster-entry {
      background: #222;
      border: 1px solid #333;
      border-radius: 4px;
      padding: 4px 10px;
      font-size: 12px;
      color: #ccc;
    }
    .ge-link { color: #7cb3f5; text-decoration: none; font-family: monospace; font-size: 11px; margin-right: 4px; }
    .ge-link:hover { text-decoration: underline; }
    .loading { color: #888; text-align: center; padding: 40px; }
    .empty { color: #666; text-align: center; padding: 40px; }
    .metric { display: inline-block; background: #222; border: 1px solid #333; border-radius: 6px; padding: 8px 14px; }
    .metric-value { font-size: 20px; font-weight: 700; color: #7cb3f5; }
    .metric-label { font-size: 11px; color: #888; margin-top: 2px; }
  `;

  @property() domain = '';
  @state() private result: any = null;
  @state() private loading = true;

  connectedCallback() { super.connectedCallback(); if (this.domain) this.load(); }
  updated(changed: Map<string, unknown>) { if (changed.has('domain') && this.domain) this.load(); }

  private async load() {
    this.loading = true;
    try {
      const res = await fetch(`/api/analysis/coverage/${encodeURIComponent(this.domain)}`);
      if (res.ok) this.result = await res.json();
    } catch (_) { /* ignore */ }
    this.loading = false;
  }

  private extractGeId(s: string): string { return s?.match(/(GE-[^.]+)/)?.[1] || s || ''; }

  render() {
    if (this.loading) return html`<div class="loading">Loading coverage analysis...</div>`;
    if (!this.result) return html`<div class="empty">No data.</div>`;
    const r = this.result;
    return html`
      <a class="back" href="#domain/${this.domain}">&larr; Back to ${this.domain}</a>
      <h2>Coverage Density — ${this.domain}</h2>
      <div class="summary">
        <div class="metric">
          <div class="metric-value">${r.clusterCount}</div>
          <div class="metric-label">Clusters</div>
        </div>
        <div class="metric">
          <div class="metric-value">${r.entryCount}</div>
          <div class="metric-label">Entries</div>
        </div>
        <div class="metric">
          <div class="metric-value">${r.spreadMetric?.toFixed(4) ?? '—'}</div>
          <div class="metric-label">Avg intra-cluster distance</div>
        </div>
      </div>
      ${r.clusters?.length === 0 ? html`<div class="empty">All entries are noise — no dense clusters found.</div>` :
        r.clusters?.map((c: any) => html`
          <div class="cluster">
            <div class="cluster-header">Cluster ${c.id + 1} — ${c.size} entries</div>
            <div class="cluster-entries">
              ${c.titles?.map((t: string, i: number) => html`
                <div class="cluster-entry">
                  <a class="ge-link" href="#entry/${this.extractGeId(c.entryIds[i])}">${this.extractGeId(c.entryIds[i])}</a>
                  ${t}
                </div>
              `)}
            </div>
          </div>
        `)}
    `;
  }
}
