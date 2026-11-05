import { describe, expect, it } from "vitest";
import { computeStats, type StatLayer } from "../../src/sim/stats/computeStats";

describe("属性汇总", () => {
  it("先加平值再乘百分比，并保留来源", () => {
    const layers: StatLayer[] = [
      { layer: "base", sourceId: "character:bladeguard", mods: [{ stat: "ad", flat: 10 }] },
      {
        layer: "item",
        sourceId: "item:sword",
        mods: [
          { stat: "ad", flat: 4 },
          { stat: "ad", pct: 0.1 },
        ],
      },
    ];
    const stats = computeStats(layers, { gold: 0 });
    expect(stats.values.ad).toBeCloseTo(15.4);
    expect(stats.sources.ad.map((s) => s.sourceId)).toEqual(["character:bladeguard", "item:sword", "item:sword"]);
  });

  it("金币变化后重算依赖持有金币的属性", () => {
    const layers: StatLayer[] = [
      { layer: "base", sourceId: "character:bladeguard", mods: [{ stat: "ad", flat: 10 }] },
      {
        layer: "talent",
        sourceId: "talent:gilded",
        mods: (ctx) => [{ stat: "ad", flat: Math.min(40, Math.floor(ctx.gold / 20)) }],
      },
    ];
    expect(computeStats(layers, { gold: 0 }).values.ad).toBe(10);
    expect(computeStats(layers, { gold: 45 }).values.ad).toBe(12);
  });
});
