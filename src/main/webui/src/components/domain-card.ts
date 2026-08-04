import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import './health-bar';
import { Segment } from './health-bar';

const TYPE_COLORS: Record<string, string> = {
  gotcha: '#e06c60',
  technique: '#6ca0dc',
  undocumented: '#d4a843',
  convention: '#6abf69',
  architectural: '#b07cd8',
  breaking: '#e04040',
};

@customElement('grove-domain-card')
export class DomainCard extends LitElement {
  static styles = css`
    :host { display: block; }
    .card {
      background: #2a2a2a;
      border: 1px solid #3a3a3a;
      border-radius: 8px;
      padding: 16px;
      cursor: pointer;
      transition: border-color 0.2s, background 0.2s;
    }
    .card:hover {
      border-color: #555;
      background: #2f2f2f;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      margin-bottom: 12px;
    }
    .domain-name {
      font-size: 16px;
      font-weight: 600;
      color: #e0e0e0;
    }
    .entry-count {
      font-size: 22px;
      font-weight: 700;
      color: #7cb3f5;
    }
    .stats {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      margin-top: 12px;
      font-size: 12px;
      color: #999;
    }
    .stat-label { color: #777; }
    .stat-value { color: #bbb; text-align: right; }
    .score-warn { color: #e06c60; }
    .staleness-good { color: #6abf69; }
    .staleness-warn { color: #d4a843; }
    .staleness-bad { color: #e06c60; }
    .bar-section { margin-top: 10px; }
  `;

  @property({ type: Object }) stats: any = {};

  private get typeSegments(): Segment[] {
    const breakdown = this.stats.typeBreakdown || {};
    return Object.entries(breakdown).map(([label, count]) => ({
      label,
      count: count as number,
      color: TYPE_COLORS[label] || '#888',
    }));
  }

  render() {
    const s = this.stats;
    const avgScore = s.averageScore?.toFixed(1) ?? '—';
    const scoreClass = s.averageScore < 9 ? 'score-warn' : '';
    const retrievalPct = s.entryCount > 0
      ? Math.round((s.retrievedEntryCount / s.entryCount) * 100)
      : 0;

    return html`
      <div class="card" @click=${this.navigate}>
        <div class="header">
          <span class="domain-name">${s.domain}</span>
          <span class="entry-count">${s.entryCount}</span>
        </div>
        <div class="bar-section">
          <grove-health-bar .segments=${this.typeSegments}></grove-health-bar>
        </div>
        <div class="stats">
          <span class="stat-label">Avg score</span>
          <span class="stat-value ${scoreClass}">${avgScore}</span>
          <span class="stat-label">Retrieved</span>
          <span class="stat-value">${retrievalPct}%</span>
          <span class="stat-label">Never retrieved</span>
          <span class="stat-value">${s.neverRetrievedCount ?? 0}</span>
        </div>
      </div>
    `;
  }

  private navigate() {
    location.hash = `domain/${this.stats.domain}`;
  }
}
