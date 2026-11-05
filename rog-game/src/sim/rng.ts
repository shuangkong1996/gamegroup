export interface RandomSource {
  next(): number;
}

/** 可复现的随机数。逻辑层禁止使用 Math.random。 */
export class Rng {
  constructor(private state: number) {
    if (this.state === 0) this.state = 1;
  }

  next(): number {
    this.state |= 0;
    this.state = (this.state + 0x6d2b79f5) | 0;
    let t = Math.imul(this.state ^ (this.state >>> 15), 1 | this.state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  getState(): number {
    return this.state >>> 0;
  }
}
