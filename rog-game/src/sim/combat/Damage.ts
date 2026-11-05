export type DamageType = "physical" | "magic" | "true";

export type DamageTag = "attack" | "skill" | "dot" | "thorns" | "proc" | "combo" | "environment";

export interface DamagePacket {
  amount: number;
  type: DamageType;
  tags: DamageTag[];
  sourceId: string;
  chain: string[];
  owner: number;
  target: number;
  crit: boolean;
  procRatio: number;
  depth: number;
  /** 结算第 1 步叠加上去的伤害加成，0.15 表示 +15%。 */
  dmgPct: number;
}

export interface Shield {
  amount: number;
  expiresAt: number;
}

export interface Combatant {
  id: number;
  hp: number;
  maxHp: number;
  armor: number;
  mr: number;
  dr: number;
  ov: number;
  crit: number;
  critDmg: number;
  shields: Shield[];
  alive: boolean;
}

export function hasTag(packet: DamagePacket, tag: DamageTag): boolean {
  return packet.tags.includes(tag);
}

export function mitigate(amount: number, resist: number): number {
  if (resist >= 0) return amount * (1 - resist / (resist + 100));
  return amount * (1 - resist / (100 - resist));
}
