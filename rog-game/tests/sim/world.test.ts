import { describe, expect, it } from "vitest";
import { bladeguard } from "../../src/content/bladeguard";
import { createWorld } from "../../src/sim/World";

describe("刃卫", () => {
  it("走近的敌人会被弧形普攻打掉", () => {
    const world = createWorld({ seed: 2, profile: bladeguard, enemyCount: 8 });
    const idle = { moveX: 0, moveY: 0, aimX: 0, aimY: 0, hasAim: false, skill: false };
    for (let i = 0; i < 60 * 6; i++) world.step(1 / 60, idle);
    expect(world.snapshot().killed).toBeGreaterThan(0);
  });
});
