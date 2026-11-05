import { describe, expect, it } from "vitest";
import type { Combatant, DamagePacket } from "../../src/sim/combat/Damage";
import { CombatPipeline } from "../../src/sim/combat/pipeline";
import { derive } from "../../src/sim/combat/procGuard";
import { Rng } from "../../src/sim/rng";

function fighter(partial: Partial<Combatant> & { id: number }): Combatant {
  return {
    hp: 100,
    maxHp: 100,
    armor: 0,
    mr: 0,
    dr: 0,
    ov: 0,
    crit: 0,
    critDmg: 1.5,
    shields: [],
    alive: true,
    ...partial,
  };
}

function packet(partial: Partial<DamagePacket> = {}): DamagePacket {
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
    ...partial,
  };
}

describe("伤害管道", () => {
  it("物理伤害吃护甲，再吃有上限的减伤", () => {
    const units = new Map<number, Combatant>([
      [1, fighter({ id: 1 })],
      [2, fighter({ id: 2, armor: 100, dr: 0.5 })],
    ]);
    const pipe = new CombatPipeline(units, new Rng(1));
    pipe.deal(packet({ amount: 100 }));
    expect(units.get(2)?.hp).toBeCloseTo(70);
  });

  it("负护甲提高受到的伤害", () => {
    const units = new Map<number, Combatant>([
      [1, fighter({ id: 1 })],
      [2, fighter({ id: 2, armor: -50 })],
    ]);
    new CombatPipeline(units, new Rng(1)).deal(packet({ amount: 90 }));
    expect(units.get(2)?.hp).toBe(0);
  });

  it("真实伤害不吃护甲和减伤，环境伤害不吃护盾", () => {
    const units = new Map<number, Combatant>([
      [1, fighter({ id: 1 })],
      [2, fighter({ id: 2, armor: 100, dr: 0.4, shields: [{ amount: 50, expiresAt: 5 }] })],
    ]);
    const pipe = new CombatPipeline(units, new Rng(1));
    pipe.deal(packet({ amount: 30, type: "true", tags: ["skill"] }));
    expect(units.get(2)?.hp).toBe(100);
    expect(units.get(2)?.shields[0].amount).toBe(20);

    const env = fighter({ id: 3, shields: [{ amount: 40, expiresAt: 9 }] });
    const units2 = new Map<number, Combatant>([
      [1, fighter({ id: 1 })],
      [3, env],
    ]);
    new CombatPipeline(units2, new Rng(1)).deal(
      packet({ amount: 10, target: 3, type: "true", tags: ["environment"] }),
    );
    expect(env.hp).toBe(90);
    expect(env.shields[0].amount).toBe(40);
  });

  it("护盾按剩余时间从短到长吸收", () => {
    const target = fighter({
      id: 2,
      shields: [
        { amount: 5, expiresAt: 10 },
        { amount: 5, expiresAt: 3 },
      ],
    });
    const units = new Map<number, Combatant>([
      [1, fighter({ id: 1 })],
      [2, target],
    ]);
    new CombatPipeline(units, new Rng(1)).deal(packet({ amount: 7, tags: ["skill"] }));
    expect(target.hp).toBe(100);
    expect(target.shields).toEqual([{ amount: 3, expiresAt: 10 }]);
  });

  it("暴击只作用于带 attack 的伤害", () => {
    const always = { next: () => 0, getState: () => 0 };
    const victim = fighter({ id: 2 });
    const units = new Map<number, Combatant>([
      [1, fighter({ id: 1, crit: 1, critDmg: 2 })],
      [2, victim],
    ]);
    new CombatPipeline(units, always).deal(packet({ amount: 10 }));
    expect(victim.hp).toBe(80);

    const other = fighter({ id: 3 });
    const units2 = new Map<number, Combatant>([
      [1, fighter({ id: 1, crit: 1, critDmg: 2 })],
      [3, other],
    ]);
    new CombatPipeline(units2, always).deal(packet({ amount: 10, target: 3, tags: ["skill"] }));
    expect(other.hp).toBe(90);
  });

  it("持续伤害的吸血只有三分之一，环境伤害不吸血也不触发荆棘", () => {
    const attacker = fighter({ id: 1, hp: 50, ov: 0.3 });
    const target = fighter({ id: 2 });
    const units = new Map<number, Combatant>([
      [1, attacker],
      [2, target],
    ]);
    let thorns = 0;
    const pipe = new CombatPipeline(units, new Rng(1));
    pipe.onDamage((info) => {
      if (info.type === "onDamageTaken") thorns += 1;
    });
    pipe.deal(packet({ amount: 30, tags: ["dot", "skill"] }));
    expect(attacker.hp).toBeCloseTo(50 + 30 * 0.3 * (1 / 3));

    pipe.deal(packet({ amount: 10, tags: ["environment"], type: "true" }));
    expect(thorns).toBe(1);
    expect(attacker.hp).toBeCloseTo(50 + 3);
  });

  it("目标死亡时取消它还没打出的荆棘", () => {
    const attacker = fighter({ id: 1, hp: 80 });
    const target = fighter({ id: 2, hp: 5 });
    const units = new Map<number, Combatant>([
      [1, attacker],
      [2, target],
    ]);
    const pipe = new CombatPipeline(units, new Rng(1));
    pipe.onDamage((info) => {
      if (info.type !== "onDamageTaken" || info.target.id !== 2) return;
      const next = derive(info.packet, "talent:thorns");
      if (!next) return;
      next.amount = 40;
      next.owner = 2;
      next.target = 1;
      next.type = "magic";
      next.tags = ["thorns"];
      info.retaliate(next);
    });
    pipe.deal(packet({ amount: 20 }));
    expect(target.alive).toBe(false);
    expect(attacker.hp).toBe(80);
  });
});
