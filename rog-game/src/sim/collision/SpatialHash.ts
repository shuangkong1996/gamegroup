export interface Point {
  x: number;
  y: number;
}

export class SpatialHash<T extends Point> {
  private cells = new Map<string, T[]>();

  constructor(private readonly cell: number) {}

  clear(): void {
    this.cells.clear();
  }

  insert(item: T): void {
    const key = this.key(item.x, item.y);
    const list = this.cells.get(key);
    if (list) list.push(item);
    else this.cells.set(key, [item]);
  }

  query(x: number, y: number, radius: number, out: T[]): void {
    const minX = Math.floor((x - radius) / this.cell);
    const maxX = Math.floor((x + radius) / this.cell);
    const minY = Math.floor((y - radius) / this.cell);
    const maxY = Math.floor((y + radius) / this.cell);
    const r2 = radius * radius;
    for (let cx = minX; cx <= maxX; cx++) {
      for (let cy = minY; cy <= maxY; cy++) {
        const list = this.cells.get(`${cx},${cy}`);
        if (!list) continue;
        for (const item of list) {
          const dx = item.x - x;
          const dy = item.y - y;
          if (dx * dx + dy * dy <= r2) out.push(item);
        }
      }
    }
  }

  private key(x: number, y: number): string {
    return `${Math.floor(x / this.cell)},${Math.floor(y / this.cell)}`;
  }
}
