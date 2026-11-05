import type { Combatant, DamagePacket } from "./Damage";
import { hasTag, mitigate } from "./Damage";
import type { RandomSource } from "../rng";

export interface DamageEvent {
  type: "onDamageDealt" | "onDamageTaken" | "onShieldAbsorb" | "onKill";
  packet: DamagePacket;
  dealt: number;
  target: Combatant;
  attacker: Combatant | undefined;
}

type Listener = (info: DamageEvent & { retaliate: (packet: DamagePacket) => void }) => void;

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n));
}

/**
 * 伤害结算。顺序与设计文档一致：加成、暴击、抗性、减伤、护盾、生命、事件、吸血。
 * environment 不吃护盾，不触发反击、吸血和 onDamageDealt。
 */
export class CombatPipeline {
  private queue: DamagePacket[] = [];
  private listeners: Listener[] = [];
  private flushing = false;

  constructor(
    private readonly units: Map<number, Combatant>,
    private readonly rng: RandomSource,
  ) {}

  onDamage(listener: Listener): void {
    this.listeners.push(listener);
  }

  deal(packet: DamagePacket): void {
    this.queue.push(packet);
    if (!this.flushing) this.flush();
  }

  private flush(): void {
    this.flushing = true;
    while (this.queue.length > 0) {
      const packet = this.queue.shift();
      if (packet) this.apply(packet);
    }
    this.flushing = false;
  }

  private apply(packet: DamagePacket): void {
    const target = this.units.get(packet.target);
    const attacker = this.units.get(packet.owner);
    if (!target?.alive) return;

    let amount = packet.amount * (1 + packet.dmgPct);
    const environment = hasTag(packet, "environment");

    if (hasTag(packet, "attack") && attacker) {
      if (!packet.crit && this.rng.next() < attacker.crit) packet.crit = true;
      if (packet.crit) amount *= attacker.critDmg;
    }

    if (packet.type === "physical") amount = mitigate(amount, target.armor);
    else if (packet.type === "magic") amount = mitigate(amount, target.mr);

    if (packet.type !== "true") amount *= 1 - clamp(target.dr, 0, 0.4);

    let absorbed = 0;
    if (!environment && amount > 0) {
      const shields = [...target.shields].filter((s) => s.amount > 0).sort((a, b) => a.expiresAt - b.expiresAt);
      for (const shield of shields) {
        if (amount <= 0) break;
        const used = Math.min(shield.amount, amount);
        shield.amount -= used;
        amount -= used;
        absorbed += used;
      }
      target.shields = target.shields.filter((s) => s.amount > 0);
    }

    const before = target.hp;
    if (amount > 0) target.hp = Math.max(0, target.hp - amount);
    const dealt = before - target.hp;
    if (target.hp <= 0) target.alive = false;

    const info = { packet, dealt, target, attacker };
    if (absorbed > 0) this.emit({ type: "onShieldAbsorb", ...info });

    const spawned: DamagePacket[] = [];
    const retaliate = (next: DamagePacket) => spawned.push(next);

    if (!environment) {
      if (attacker) this.emit({ type: "onDamageDealt", ...info }, retaliate);
      this.emit({ type: "onDamageTaken", ...info }, retaliate);
      if (!target.alive) this.emit({ type: "onKill", ...info }, retaliate);
    }

    for (const next of spawned) {
      const revenge = !target.alive && next.owner === target.id && hasTag(next, "thorns");
      if (!revenge) this.queue.push(next);
    }

    if (attacker?.alive && !environment && dealt > 0 && attacker.ov > 0) {
      const factor = hasTag(packet, "dot") ? 1 / 3 : 1;
      attacker.hp = Math.min(attacker.maxHp, attacker.hp + dealt * attacker.ov * factor);
    }
  }

  private emit(event: DamageEvent, retaliate: (packet: DamagePacket) => void = () => {}): void {
    for (const listener of this.listeners) listener({ ...event, retaliate });
  }
}
