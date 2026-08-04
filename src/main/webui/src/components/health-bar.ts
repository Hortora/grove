import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';

export interface Segment {
  label: string;
  count: number;
  color: string;
}

@customElement('grove-health-bar')
export class HealthBar extends LitElement {
  static styles = css`
    :host { display: block; }
    .bar {
      display: flex;
      height: 8px;
      border-radius: 4px;
      overflow: hidden;
      background: #333;
    }
    .segment {
      height: 100%;
      min-width: 2px;
      transition: width 0.3s ease;
    }
    .legend {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 6px;
      font-size: 11px;
      color: #999;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .legend-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
  `;

  @property({ type: Array }) segments: Segment[] = [];
  @property({ type: Boolean }) showLegend = false;

  render() {
    const total = this.segments.reduce((sum, s) => sum + s.count, 0);
    if (total === 0) return html`<div class="bar"></div>`;

    return html`
      <div class="bar">
        ${this.segments.map(s => html`
          <div class="segment"
               style="width: ${(s.count / total) * 100}%; background: ${s.color}"
               title="${s.label}: ${s.count}">
          </div>
        `)}
      </div>
      ${this.showLegend ? html`
        <div class="legend">
          ${this.segments.map(s => html`
            <span class="legend-item">
              <span class="legend-dot" style="background: ${s.color}"></span>
              ${s.label} ${s.count}
            </span>
          `)}
        </div>
      ` : ''}
    `;
  }
}
