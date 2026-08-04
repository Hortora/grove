import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

@customElement('grove-cross-domain-review')
export class CrossDomainReview extends LitElement {
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
    .domain-badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; background: rgba(108,160,220,0.15); color: #6ca0dc; }
    .suggested { background: rgba(106,191,105,0.15); color: #6abf69; }
    .delta { font-weight: 600; color: #d4a843; }
    .move-btn { background: #333; border: 1px solid #d4a843; border-radius: 4px; color: #d4a843; padding: 3px 10px; cursor: pointer; font-size: 11px; }
    .move-btn:hover { background: rgba(212,168,67,0.15); }
    .loading { color: #888; text-align: center; padding: 40px; }
    .empty { color: #666; text-align: center; padding: 40px; }
    .toast { position: fixed; bottom: 24px; right: 24px; padding: 12px 20px; border-radius: 8px; font-size: 13px; z-index: 100; }
    .toast-success { background: #1a3a1a; color: #6abf69; border: 1px solid #2a5a2a; }
    .toast-error { background: #3a1a1a; color: #e06c60; border: 1px solid #5a2a2a; }
  `;

  @state() private candidates: any[] = [];
  @state() private loading = true;
  @state() private toast = '';
  @state() private toastType = 'success';

  connectedCallback() { super.connectedCallback(); this.load(); }

  private async load() {
    this.loading = true;
    try {
      const res = await fetch('/api/analysis/cross-domain');
      if (res.ok) { const data = await res.json(); this.candidates = data.candidates || []; }
    } catch (_) { /* ignore */ }
    this.loading = false;
  }

  private extractGeId(s: string): string { return s?.match(/(GE-[^.]+)/)?.[1] || s || ''; }

  private async moveEntry(sourceDocId: string, targetDomain: string) {
    try {
      const res = await fetch(`/api/curation/move/${sourceDocId}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ targetDomain }),
      });
      if (!res.ok) throw new Error('Failed');
      this.showToast(`Moved to ${targetDomain}`, 'success');
      this.load();
    } catch (e) { this.showToast(`Error: ${e}`, 'error'); }
  }

  private showToast(msg: string, type: string) {
    this.toast = msg; this.toastType = type;
    setTimeout(() => { this.toast = ''; }, 3000);
  }

  render() {
    if (this.loading) return html`<div class="loading">Loading cross-domain analysis...</div>`;
    return html`
      <a class="back" href="#">&larr; Back to domain map</a>
      <h2>Cross-Domain Similarity</h2>
      ${this.candidates.length === 0 ? html`<div class="empty">No cross-domain candidates found.</div>` : html`
        <table>
          <thead><tr><th>GE-ID</th><th>Title</th><th>Current</th><th>Suggested</th><th>Delta</th><th></th></tr></thead>
          <tbody>
            ${this.candidates.map(c => html`
              <tr>
                <td><a class="ge-link" href="#entry/${this.extractGeId(c.sourceDocumentId)}">${this.extractGeId(c.sourceDocumentId)}</a></td>
                <td>${c.title}</td>
                <td><span class="domain-badge">${c.currentDomain}</span></td>
                <td><span class="domain-badge suggested">${c.suggestedDomain}</span></td>
                <td><span class="delta">${c.delta.toFixed(4)}</span></td>
                <td><button class="move-btn" @click=${() => this.moveEntry(c.sourceDocumentId, c.suggestedDomain)}>Move</button></td>
              </tr>
            `)}
          </tbody>
        </table>
      `}
      ${this.toast ? html`<div class="toast toast-${this.toastType}">${this.toast}</div>` : ''}
    `;
  }
}
