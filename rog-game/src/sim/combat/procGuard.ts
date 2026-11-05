import type { DamagePacket, DamageTag } from "./Damage";

/** 派生连锁伤害。深度超过 4，或来源已经在链上时，返回 null。 */
export function derive(parent: DamagePacket, sourceId: string, ratio = 0.7): DamagePacket | null {
  if (parent.depth + 1 > 4) return null;
  if (parent.chain.includes(sourceId)) return null;
  const tags: DamageTag[] = ["proc"];
  return {
    amount: 0,
    type: parent.type,
    tags,
    sourceId,
    chain: [...parent.chain, sourceId],
    owner: parent.owner,
    target: parent.target,
    crit: false,
    procRatio: parent.procRatio * ratio,
    depth: parent.depth + 1,
    dmgPct: 0,
  };
}
