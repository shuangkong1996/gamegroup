import { describe, expect, it } from "vitest";
import type { DamagePacket } from "../../src/sim/combat/Damage";
import { derive } from "../../src/sim/combat/procGuard";

function root(): DamagePacket {
  return {
    amount: 10,
    type: "physical",
    tags: ["attack"],
    sourceId: "skill:auto",
    chain: ["skill:auto"],
    owner: 1,
    target: 2,
    crit: false,
    procRatio: 1,
    depth: 0,
    dmgPct: 0,
  };
}

describe("连锁衰减", () => {
  it("最多派生到深度 4，并且每次衰减 0.7", () => {
    let current = root();
    for (let i = 1; i <= 4; i++) {
      const next = derive(current, `talent:chain-${i}`);
      expect(next).not.toBeNull();
      expect(next?.depth).toBe(i);
      expect(next?.procRatio).toBeCloseTo(0.7 ** i);
      current = next!;
    }
    expect(derive(current, "talent:chain-5")).toBeNull();
  });

  it("同一个来源不能出现在自己的链上", () => {
    const first = derive(root(), "talent:thorns");
    expect(first).not.toBeNull();
    expect(derive(first!, "talent:thorns")).toBeNull();
    expect(derive(first!, "skill:auto")).toBeNull();
  });
});
