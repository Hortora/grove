import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

@customElement('grove-entry-table')
export class EntryTable extends LitElement {
  static styles = css`
    :host { display: block; }
    .controls {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
      flex-wrap: wrap;
      align-items: center;
    }
    select, input {
      background: #2a2a2a;
      color: #ccc;
      border: 1px solid #444;
      border-radius: 4px;
      padding: 6px 10px;
      font-size: 13px;
    }
    label {
      font-size: 12px;
      color: #888;
      margin-right: 4px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
    }
    th {
      text-align: left;
      padding: 8px 12px;
      border-bottom: 2px solid #3a3a3a;
      color: #999;
      font-weight: 500;
      cursor: pointer;
      user-select: none;
      white-space: nowrap;
    }
    th:hover { color: #ccc; }
    th.active { color: #7cb3f5; }
    th .arrow { font-size: 10px; margin-left: 4px; }
    td {
      padding: 8px 12px;
      border-bottom: 1px solid #2a2a2a;
      color: #ccc;
    }
    tr:hover td { background: #2a2a2a; }
    .ge-link {
      color: #7cb3f5;
      text-decoration: none;
      font-family: monospace;
      font-size: 12px;
    }
    .ge-link:hover { text-decoration: underline; }
    .badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 500;
    }
    .badge-gotcha { background: rgba(224,108,96,0.2); color: #e06c60; }
    .badge-technique { background: rgba(108,160,220,0.2); color: #6ca0dc; }
    .badge-undocumented { background: rgba(212,168,67,0.2); color: #d4a843; }
    .badge-convention { background: rgba(106,191,105,0.2); color: #6abf69; }
    .badge-architectural { background: rgba(176,124,216,0.2); color: #b07cd8; }
    .badge-breaking { background: rgba(224,64,64,0.2); color: #e04040; }
    .badge-reference { background: rgba(150,150,150,0.2); color: #aaa; }
    .staleness-current { color: #6abf69; }
    .staleness-aging { color: #d4a843; }
    .staleness-stale { color: #e06c60; }
    .staleness-unknown { color: #666; }
    .score { font-weight: 600; }
    .score-low { color: #e06c60; }
    .empty { color: #666; padding: 40px; text-align: center; }
  `;

  @property({ type: Array }) entries: any[] = [];
  @state() private sortCol = 'score';
  @state() private sortAsc = false;
  @state() private filterType = '';
  @state() private filterStale = '';

  private get types(): string[] {
    const types = new Set<string>();
    this.entries.forEach(e => { if (e.type) types.add(e.type); });
    return [...types].sort();
  }

  private get filteredAndSorted(): any[] {
    let result = [...this.entries];

    if (this.filterType) {
      result = result.filter(e => e.type === this.filterType);
    }
    if (this.filterStale) {
      const wantStale = this.filterStale === 'stale';
      result = result.filter(e => (e.stalenessStatus === 'stale') === wantStale);
    }

    result.sort((a, b) => {
      let cmp = 0;
      switch (this.sortCol) {
        case 'score': cmp = (a.score ?? 0) - (b.score ?? 0); break;
        case 'title': cmp = (a.title ?? '').localeCompare(b.title ?? ''); break;
        case 'submitted': cmp = (a.submitted ?? '').localeCompare(b.submitted ?? ''); break;
        case 'staleness': cmp = (a.stalenessStatus ?? '').localeCompare(b.stalenessStatus ?? ''); break;
        case 'type': cmp = (a.type ?? '').localeCompare(b.type ?? ''); break;
      }
      return this.sortAsc ? cmp : -cmp;
    });

    return result;
  }

  private toggleSort(col: string) {
    if (this.sortCol === col) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortCol = col;
      this.sortAsc = col === 'title';
    }
  }

  private headerCell(col: string, label: string) {
    const active = this.sortCol === col;
    const arrow = active ? (this.sortAsc ? '▲' : '▼') : '';
    return html`<th class="${active ? 'active' : ''}" @click=${() => this.toggleSort(col)}>
      ${label}<span class="arrow">${arrow}</span>
    </th>`;
  }

  private extractGeId(sourceDocumentId: string): string {
    if (!sourceDocumentId) return '';
    const match = sourceDocumentId.match(/(GE-[^.]+)/);
    return match ? match[1] : sourceDocumentId;
  }

  render() {
    const rows = this.filteredAndSorted;

    return html`
      <div class="controls">
        <div>
          <label>Type</label>
          <select @change=${(e: Event) => this.filterType = (e.target as HTMLSelectElement).value}>
            <option value="">All</option>
            ${this.types.map(t => html`<option value=${t}>${t}</option>`)}
          </select>
        </div>
        <div>
          <label>Staleness</label>
          <select @change=${(e: Event) => this.filterStale = (e.target as HTMLSelectElement).value}>
            <option value="">All</option>
            <option value="stale">Stale only</option>
            <option value="fresh">Fresh only</option>
          </select>
        </div>
        <div style="color: #666; font-size: 12px; margin-left: auto;">
          ${rows.length} entries
        </div>
      </div>
      ${rows.length === 0 ? html`<div class="empty">No entries match the current filters.</div>` : html`
        <table>
          <thead>
            <tr>
              <th>GE-ID</th>
              ${this.headerCell('title', 'Title')}
              ${this.headerCell('type', 'Type')}
              ${this.headerCell('score', 'Score')}
              ${this.headerCell('submitted', 'Submitted')}
              ${this.headerCell('staleness', 'Staleness')}
            </tr>
          </thead>
          <tbody>
            ${rows.map(e => html`
              <tr>
                <td><a class="ge-link" href="#entry/${this.extractGeId(e.sourceDocumentId)}">${this.extractGeId(e.sourceDocumentId)}</a></td>
                <td>${e.title}</td>
                <td><span class="badge badge-${e.type ?? 'reference'}">${e.type}</span></td>
                <td><span class="score ${e.score < 9 ? 'score-low' : ''}">${e.score?.toFixed(0)}</span></td>
                <td>${e.submitted ?? '—'}</td>
                <td><span class="staleness-${e.stalenessStatus ?? 'unknown'}">${e.stalenessStatus ?? 'unknown'}</span></td>
              </tr>
            `)}
          </tbody>
        </table>
      `}
    `;
  }
}
