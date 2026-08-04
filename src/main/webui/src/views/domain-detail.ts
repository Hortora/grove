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
    .bulk-bar {
      display: flex;
      gap: 8px;
      align-items: center;
      padding: 10px 16px;
      background: #1a2a3a;
      border: 1px solid #2a4a6a;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 13px;
    }
    .bulk-bar .count { color: #7cb3f5; font-weight: 600; margin-right: 8px; }
    .bulk-btn {
      background: #2a2a2a;
      border: 1px solid #444;
      border-radius: 4px;
      color: #ccc;
      padding: 5px 12px;
      cursor: pointer;
      font-size: 12px;
    }
    .bulk-btn:hover { border-color: #7cb3f5; }
    .bulk-btn.confirm { border-color: #6abf69; color: #6abf69; }
    .bulk-btn.retire { border-color: #e06c60; color: #e06c60; }
    .bulk-btn.retag { border-color: #d4a843; color: #d4a843; }
    .bulk-input {
      background: #2a2a2a;
      color: #ccc;
      border: 1px solid #444;
      border-radius: 4px;
      padding: 5px 8px;
      font-size: 12px;
    }
    .toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      padding: 12px 20px;
      border-radius: 8px;
      font-size: 13px;
      z-index: 100;
      animation: fadeIn 0.3s ease;
    }
    .toast-success { background: #1a3a1a; color: #6abf69; border: 1px solid #2a5a2a; }
    .toast-error { background: #3a1a1a; color: #e06c60; border: 1px solid #5a2a2a; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `;

  @property() domain = '';
  @state() private entries: any[] = [];
  @state() private loading = true;
  @state() private error = '';
  @state() private selectedEntries: string[] = [];
  @state() private showRetireInput = false;
  @state() private retireReason = '';
  @state() private showRetagInput = false;
  @state() private retagAdd = '';
  @state() private retagRemove = '';
  @state() private toast = '';
  @state() private toastType = 'success';

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

  private onSelectionChanged(e: CustomEvent) {
    this.selectedEntries = e.detail.selected;
    this.showRetireInput = false;
    this.showRetagInput = false;
  }

  private async bulkConfirm() {
    try {
      const res = await fetch('/api/curation/bulk/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ entries: this.selectedEntries }),
      });
      const data = await res.json();
      if (data.status !== 'ok') throw new Error(data.message || 'Failed');
      this.showToast(`Confirmed ${data.count} entries`, 'success');
      this.resetBulk();
      this.loadEntries();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private async submitBulkRetire() {
    if (!this.retireReason.trim()) return;
    try {
      const res = await fetch('/api/curation/bulk/retire', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ entries: this.selectedEntries, reason: this.retireReason }),
      });
      const data = await res.json();
      if (data.status !== 'ok') throw new Error(data.message || 'Failed');
      this.showToast(`Retired ${data.count} entries`, 'success');
      this.resetBulk();
      this.loadEntries();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private async submitBulkRetag() {
    const addTags = this.retagAdd.split(',').map(t => t.trim()).filter(Boolean);
    const removeTags = this.retagRemove.split(',').map(t => t.trim()).filter(Boolean);
    if (addTags.length === 0 && removeTags.length === 0) return;
    try {
      const res = await fetch('/api/curation/bulk/retag', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ entries: this.selectedEntries, addTags, removeTags }),
      });
      const data = await res.json();
      if (data.status !== 'ok') throw new Error(data.message || 'Failed');
      this.showToast(`Retagged ${data.count} entries`, 'success');
      this.resetBulk();
      this.loadEntries();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private resetBulk() {
    this.selectedEntries = [];
    this.showRetireInput = false;
    this.showRetagInput = false;
    this.retireReason = '';
    this.retagAdd = '';
    this.retagRemove = '';
    const table = this.shadowRoot?.querySelector('grove-entry-table') as any;
    table?.clearSelection();
  }

  private showToast(msg: string, type: string) {
    this.toast = msg;
    this.toastType = type;
    setTimeout(() => { this.toast = ''; }, 3000);
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
        <a class="back" href="#duplicates/${this.domain}" style="margin-left: auto;">Duplicates &rarr;</a>
        <a class="back" href="#outliers/${this.domain}">Outliers &rarr;</a>
        <a class="back" href="#cross-domain">Cross-Domain &rarr;</a>
      </div>
      <div class="summary">
        <div>${this.entries.length} entries</div>
        <div>Avg score: <span>${avgScore}</span></div>
        <div>Stale: <span>${staleCount}</span></div>
      </div>
      <div class="bar-section">
        <grove-health-bar .segments=${this.typeSegments} .showLegend=${true}></grove-health-bar>
      </div>
      ${this.selectedEntries.length > 0 ? html`
        <div class="bulk-bar">
          <span class="count">${this.selectedEntries.length} selected</span>
          <button class="bulk-btn confirm" @click=${this.bulkConfirm}>Confirm Freshness</button>
          ${this.showRetireInput ? html`
            <input class="bulk-input" placeholder="Reason..." .value=${this.retireReason} @input=${(e: Event) => this.retireReason = (e.target as HTMLInputElement).value} @keydown=${(e: KeyboardEvent) => e.key === 'Enter' && this.submitBulkRetire()}>
            <button class="bulk-btn retire" @click=${this.submitBulkRetire}>Confirm Retire</button>
          ` : html`
            <button class="bulk-btn retire" @click=${() => this.showRetireInput = true}>Retire</button>
          `}
          ${this.showRetagInput ? html`
            <input class="bulk-input" placeholder="Add tags (comma-sep)" .value=${this.retagAdd} @input=${(e: Event) => this.retagAdd = (e.target as HTMLInputElement).value}>
            <input class="bulk-input" placeholder="Remove tags (comma-sep)" .value=${this.retagRemove} @input=${(e: Event) => this.retagRemove = (e.target as HTMLInputElement).value}>
            <button class="bulk-btn retag" @click=${this.submitBulkRetag}>Apply Tags</button>
          ` : html`
            <button class="bulk-btn retag" @click=${() => this.showRetagInput = true}>Re-tag</button>
          `}
        </div>
      ` : ''}
      <grove-entry-table .entries=${this.entries} .selectable=${true} @selection-changed=${this.onSelectionChanged}></grove-entry-table>
      ${this.toast ? html`<div class="toast toast-${this.toastType}">${this.toast}</div>` : ''}
    `;
  }
}
