import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('grove-entry-detail')
export class EntryDetail extends LitElement {
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
    .layout {
      display: grid;
      grid-template-columns: 1fr 280px;
      gap: 24px;
    }
    @media (max-width: 800px) {
      .layout { grid-template-columns: 1fr; }
    }
    h2 {
      margin: 0 0 8px 0;
      font-size: 20px;
      font-weight: 600;
      color: #e0e0e0;
    }
    .badges {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .badge {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    .badge-domain { background: rgba(108,160,220,0.15); color: #6ca0dc; }
    .badge-type-gotcha { background: rgba(224,108,96,0.15); color: #e06c60; }
    .badge-type-technique { background: rgba(108,160,220,0.15); color: #6ca0dc; }
    .badge-type-undocumented { background: rgba(212,168,67,0.15); color: #d4a843; }
    .badge-type-convention { background: rgba(106,191,105,0.15); color: #6abf69; }
    .badge-score { background: rgba(124,179,245,0.15); color: #7cb3f5; }
    .badge-staleness-current { background: rgba(106,191,105,0.15); color: #6abf69; }
    .badge-staleness-aging { background: rgba(212,168,67,0.15); color: #d4a843; }
    .badge-staleness-stale { background: rgba(224,108,96,0.15); color: #e06c60; }
    .badge-staleness-unknown { background: rgba(100,100,100,0.15); color: #888; }
    .badge-verified { background: rgba(106,191,105,0.15); color: #6abf69; }
    .content {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 8px;
      padding: 20px;
      font-size: 14px;
      line-height: 1.7;
      color: #ccc;
      white-space: pre-wrap;
      overflow-x: auto;
    }
    .sidebar {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .sidebar-section {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 8px;
      padding: 16px;
    }
    .sidebar-section h3 {
      margin: 0 0 12px 0;
      font-size: 13px;
      font-weight: 600;
      color: #999;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .meta-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      padding: 4px 0;
      border-bottom: 1px solid #333;
    }
    .meta-row:last-child { border-bottom: none; }
    .meta-label { color: #777; }
    .meta-value { color: #bbb; text-align: right; max-width: 160px; word-break: break-word; }
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 8px;
    }
    .tag {
      background: #333;
      color: #aaa;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;
    }
    button {
      width: 100%;
      padding: 10px;
      border: 1px solid #444;
      border-radius: 6px;
      background: #333;
      color: #ccc;
      font-size: 13px;
      cursor: pointer;
      transition: background 0.2s;
    }
    button:hover { background: #444; }
    button.confirm { border-color: #6abf69; color: #6abf69; }
    button.confirm:hover { background: rgba(106,191,105,0.15); }
    button.retire { border-color: #e06c60; color: #e06c60; }
    button.retire:hover { background: rgba(224,108,96,0.15); }
    button.edit { border-color: #7cb3f5; color: #7cb3f5; }
    button.edit:hover { background: rgba(124,179,245,0.15); }
    button.move { border-color: #d4a843; color: #d4a843; }
    button.move:hover { background: rgba(212,168,67,0.15); }
    select.domain-picker {
      width: 100%;
      background: #2a2a2a;
      color: #ccc;
      border: 1px solid #444;
      border-radius: 4px;
      padding: 8px;
      font-size: 13px;
      margin-bottom: 8px;
      box-sizing: border-box;
    }
    button.save { border-color: #6abf69; color: #6abf69; }
    button.cancel { border-color: #888; color: #888; }
    textarea {
      width: 100%;
      min-height: 400px;
      background: #2a2a2a;
      color: #ccc;
      border: 1px solid #444;
      border-radius: 8px;
      padding: 16px;
      font-family: monospace;
      font-size: 13px;
      line-height: 1.6;
      resize: vertical;
      box-sizing: border-box;
    }
    input.reason {
      width: 100%;
      background: #2a2a2a;
      color: #ccc;
      border: 1px solid #444;
      border-radius: 4px;
      padding: 8px;
      font-size: 13px;
      margin-bottom: 8px;
      box-sizing: border-box;
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
    .loading { color: #888; padding: 40px; text-align: center; }
    .error { color: #e06c60; padding: 20px; background: #2a1a1a; border-radius: 8px; }
  `;

  @property() geId = '';
  @state() private entry: any = null;
  @state() private loading = true;
  @state() private error = '';
  @state() private editing = false;
  @state() private editContent = '';
  @state() private showRetireInput = false;
  @state() private retireReason = '';
  @state() private toast = '';
  @state() private toastType = 'success';
  @state() private showMovePicker = false;
  @state() private domains: string[] = [];
  @state() private selectedDomain = '';

  updated(changed: Map<string, unknown>) {
    if (changed.has('geId') && this.geId) {
      this.loadEntry();
    }
  }

  private async loadEntry() {
    this.loading = true;
    this.error = '';
    this.editing = false;
    this.showRetireInput = false;
    try {
      const res = await fetch(`/api/entries/${encodeURIComponent(this.geId)}`);
      if (!res.ok) throw new Error(res.status === 404 ? 'Entry not found' : `API error: ${res.status}`);
      this.entry = await res.json();
    } catch (e) {
      this.error = `${e}`;
    } finally {
      this.loading = false;
    }
  }

  private async confirmFreshness() {
    try {
      const res = await fetch(`/api/curation/confirm/${this.entry.sourceDocumentId}`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed');
      this.showToast('Freshness confirmed', 'success');
      this.loadEntry();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private async submitRetire() {
    if (!this.retireReason.trim()) return;
    try {
      const res = await fetch(`/api/curation/retire/${this.entry.sourceDocumentId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: this.retireReason }),
      });
      if (!res.ok) throw new Error('Failed');
      this.showToast('Entry retired', 'success');
      this.showRetireInput = false;
      this.retireReason = '';
      this.loadEntry();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private startEdit() {
    this.editing = true;
    this.editContent = this.entry.content || '';
  }

  private async saveEdit() {
    try {
      const res = await fetch(`/api/curation/edit/${this.entry.sourceDocumentId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'text/plain' },
        body: this.editContent,
      });
      if (!res.ok) throw new Error('Failed');
      this.showToast('Entry updated', 'success');
      this.editing = false;
      this.loadEntry();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private async startMove() {
    if (this.domains.length === 0) {
      const res = await fetch('/api/domains');
      if (res.ok) {
        const data = await res.json();
        this.domains = data.map((d: any) => d.domain).filter((d: string) => d !== this.entry.domain);
      }
    }
    this.showMovePicker = true;
    this.selectedDomain = '';
  }

  private async submitMove() {
    if (!this.selectedDomain) return;
    try {
      const res = await fetch(`/api/curation/move/${this.entry.sourceDocumentId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ targetDomain: this.selectedDomain }),
      });
      if (!res.ok) throw new Error('Failed');
      this.showToast(`Moved to ${this.selectedDomain}`, 'success');
      this.showMovePicker = false;
      this.loadEntry();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private showToast(msg: string, type: string) {
    this.toast = msg;
    this.toastType = type;
    setTimeout(() => { this.toast = ''; }, 3000);
  }

  render() {
    if (this.loading) return html`<div class="loading">Loading entry...</div>`;
    if (this.error) return html`<div class="error">${this.error}</div>`;

    const e = this.entry;
    const backDomain = e.domain ? `#domain/${e.domain}` : '#';

    return html`
      <a class="back" href=${backDomain}>&larr; Back to ${e.domain || 'domain map'}</a>
      <h2>${e.title}</h2>
      <div class="badges">
        <span class="badge badge-domain">${e.domain}</span>
        <span class="badge badge-type-${e.type}">${e.type}</span>
        <span class="badge badge-score">score: ${e.score}</span>
        <span class="badge badge-staleness-${e.stalenessStatus}">${e.stalenessStatus}</span>
        ${e.verified ? html`<span class="badge badge-verified">verified</span>` : ''}
        ${e.verifiedOn ? html`<span class="badge badge-staleness-current">${e.verifiedOn}</span>` : ''}
      </div>
      <div class="layout">
        <div>
          ${this.editing ? html`
            <textarea .value=${this.editContent} @input=${(ev: Event) => this.editContent = (ev.target as HTMLTextAreaElement).value}></textarea>
            <div style="display: flex; gap: 8px; margin-top: 8px;">
              <button class="save" @click=${this.saveEdit}>Save</button>
              <button class="cancel" @click=${() => this.editing = false}>Cancel</button>
            </div>
          ` : html`
            <div class="content">${e.content}</div>
          `}
        </div>
        <div class="sidebar">
          <div class="sidebar-section">
            <h3>Actions</h3>
            <div style="display: flex; flex-direction: column; gap: 8px;">
              <button class="confirm" @click=${this.confirmFreshness}>Confirm Freshness</button>
              ${this.showRetireInput ? html`
                <input class="reason" placeholder="Reason for retiring..."
                       .value=${this.retireReason}
                       @input=${(ev: Event) => this.retireReason = (ev.target as HTMLInputElement).value}
                       @keydown=${(ev: KeyboardEvent) => ev.key === 'Enter' && this.submitRetire()}>
                <button class="retire" @click=${this.submitRetire}>Confirm Retire</button>
                <button class="cancel" @click=${() => this.showRetireInput = false}>Cancel</button>
              ` : html`
                <button class="retire" @click=${() => this.showRetireInput = true}>Retire</button>
              `}
              <button class="edit" @click=${this.startEdit}>Edit</button>
              ${this.showMovePicker ? html`
                <select class="domain-picker" @change=${(ev: Event) => this.selectedDomain = (ev.target as HTMLSelectElement).value}>
                  <option value="">Select domain...</option>
                  ${this.domains.map(d => html`<option value=${d}>${d}</option>`)}
                </select>
                <button class="move" @click=${this.submitMove}>Confirm Move</button>
                <button class="cancel" @click=${() => this.showMovePicker = false}>Cancel</button>
              ` : html`
                <button class="move" @click=${this.startMove}>Move Domain</button>
              `}
            </div>
          </div>
          <div class="sidebar-section">
            <h3>Metadata</h3>
            ${this.metaRow('Author', e.author)}
            ${this.metaRow('Submitted', e.submitted)}
            ${this.metaRow('Last reviewed', e.lastReviewed || '—')}
            ${this.metaRow('Staleness threshold', e.stalenessThreshold ? `${e.stalenessThreshold} days` : '—')}
            ${this.metaRow('Source', e.sourceDocumentId)}
            ${e.constraints ? this.metaRow('Constraints', e.constraints) : ''}
            ${e.invalidationTriggers ? this.metaRow('Invalidation', e.invalidationTriggers) : ''}
          </div>
          ${e.tags && e.tags.length > 0 ? html`
            <div class="sidebar-section">
              <h3>Tags</h3>
              <div class="tags">
                ${e.tags.map((t: string) => html`<span class="tag">${t}</span>`)}
              </div>
            </div>
          ` : ''}
        </div>
      </div>
      ${this.toast ? html`<div class="toast toast-${this.toastType}">${this.toast}</div>` : ''}
    `;
  }

  private metaRow(label: string, value: string) {
    return html`
      <div class="meta-row">
        <span class="meta-label">${label}</span>
        <span class="meta-value">${value || '—'}</span>
      </div>
    `;
  }
}
