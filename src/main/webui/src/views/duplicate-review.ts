import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('grove-duplicate-review')
export class DuplicateReview extends LitElement {
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
    h2 { margin: 0 0 8px; font-size: 20px; font-weight: 600; color: #e0e0e0; }
    .summary { font-size: 13px; color: #888; margin-bottom: 16px; }
    .actions-bar {
      display: flex;
      gap: 8px;
      margin-bottom: 20px;
    }
    .analyse-btn {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 6px;
      color: #e0e0e0;
      padding: 8px 16px;
      cursor: pointer;
      font-size: 13px;
    }
    .analyse-btn:hover:not(:disabled) { border-color: #7cb3f5; }
    .analyse-btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .pair {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 12px;
    }
    .pair-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .similarity {
      font-size: 14px;
      font-weight: 600;
      color: #e06c60;
    }
    .pair-entries {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .pair-entry {
      background: #222;
      border: 1px solid #333;
      border-radius: 6px;
      padding: 12px;
    }
    .pair-entry h4 {
      margin: 0 0 4px;
      font-size: 14px;
      color: #ccc;
    }
    .ge-link {
      color: #7cb3f5;
      text-decoration: none;
      font-family: monospace;
      font-size: 12px;
    }
    .ge-link:hover { text-decoration: underline; }
    .pair-actions {
      display: flex;
      gap: 8px;
      margin-top: 12px;
    }
    .pair-btn {
      background: #333;
      border: 1px solid #444;
      border-radius: 4px;
      color: #ccc;
      padding: 5px 12px;
      cursor: pointer;
      font-size: 12px;
    }
    .pair-btn:hover { border-color: #7cb3f5; }
    .pair-btn.retire { border-color: #e06c60; color: #e06c60; }
    .empty { color: #666; text-align: center; padding: 40px; }
    .loading { color: #888; text-align: center; padding: 40px; }
    .toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      padding: 12px 20px;
      border-radius: 8px;
      font-size: 13px;
      z-index: 100;
    }
    .toast-success { background: #1a3a1a; color: #6abf69; border: 1px solid #2a5a2a; }
    .toast-error { background: #3a1a1a; color: #e06c60; border: 1px solid #5a2a2a; }
  `;

  @property() domain = '';
  @state() private pairs: any[] = [];
  @state() private loading = false;
  @state() private analysing = false;
  @state() private toast = '';
  @state() private toastType = 'success';

  connectedCallback() {
    super.connectedCallback();
    if (this.domain) this.loadCached();
  }

  updated(changed: Map<string, unknown>) {
    if (changed.has('domain') && this.domain) this.loadCached();
  }

  private async loadCached() {
    this.loading = true;
    try {
      const res = await fetch(`/api/analysis/duplicates/${encodeURIComponent(this.domain)}`);
      if (res.ok) {
        const data = await res.json();
        this.pairs = data.pairs || [];
      }
    } catch (_) { /* ignore */ }
    this.loading = false;
  }

  private async runAnalysis() {
    this.analysing = true;
    try {
      const res = await fetch(`/api/analysis/duplicates/${encodeURIComponent(this.domain)}`, { method: 'POST' });
      const data = await res.json();
      if (data.status === 'ok') {
        this.pairs = data.pairs || [];
        this.showToast(`Found ${data.count} duplicate pairs`, 'success');
      } else {
        this.showToast(`Error: ${data.message}`, 'error');
      }
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
    this.analysing = false;
  }

  private async retireEntry(sourceDocId: string, otherTitle: string) {
    try {
      const res = await fetch(`/api/curation/retire/${sourceDocId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: `Duplicate of ${otherTitle}` }),
      });
      if (!res.ok) throw new Error('Failed');
      this.showToast('Entry retired', 'success');
      await this.loadCached();
    } catch (e) {
      this.showToast(`Error: ${e}`, 'error');
    }
  }

  private extractGeId(sourceDocId: string): string {
    const match = sourceDocId?.match(/(GE-[^.]+)/);
    return match ? match[1] : sourceDocId || '';
  }

  private showToast(msg: string, type: string) {
    this.toast = msg;
    this.toastType = type;
    setTimeout(() => { this.toast = ''; }, 3000);
  }

  render() {
    return html`
      <a class="back" href="#domain/${this.domain}">&larr; Back to ${this.domain}</a>
      <h2>Near-Duplicate Detection — ${this.domain}</h2>
      <div class="summary">${this.pairs.length} pairs above 0.92 cosine similarity</div>
      <div class="actions-bar">
        <button class="analyse-btn" @click=${this.runAnalysis} ?disabled=${this.analysing}>
          ${this.analysing ? 'Analysing...' : 'Run Analysis'}
        </button>
      </div>
      ${this.loading ? html`<div class="loading">Loading cached results...</div>` :
        this.pairs.length === 0 ? html`<div class="empty">No duplicates found. Run analysis to scan.</div>` :
        this.pairs.map(p => html`
          <div class="pair">
            <div class="pair-header">
              <span>Similarity</span>
              <span class="similarity">${(p.similarity * 100).toFixed(1)}%</span>
            </div>
            <div class="pair-entries">
              <div class="pair-entry">
                <a class="ge-link" href="#entry/${this.extractGeId(p.sourceDocIdA)}">${this.extractGeId(p.sourceDocIdA)}</a>
                <h4>${p.titleA}</h4>
                <div class="pair-actions">
                  <button class="pair-btn retire" @click=${() => this.retireEntry(p.sourceDocIdA, p.titleB)}>Retire this</button>
                </div>
              </div>
              <div class="pair-entry">
                <a class="ge-link" href="#entry/${this.extractGeId(p.sourceDocIdB)}">${this.extractGeId(p.sourceDocIdB)}</a>
                <h4>${p.titleB}</h4>
                <div class="pair-actions">
                  <button class="pair-btn retire" @click=${() => this.retireEntry(p.sourceDocIdB, p.titleA)}>Retire this</button>
                </div>
              </div>
            </div>
          </div>
        `)}
      ${this.toast ? html`<div class="toast toast-${this.toastType}">${this.toast}</div>` : ''}
    `;
  }
}
