import type { CombatProfile } from "../sim/World";

export const bladeguard: CombatProfile = {
  id: "bladeguard",
  name: "刃卫",
  maxHp: 180,
  ad: 14,
  ms: 78,
  attackInterval: 0.85,
  attackRange: 36,
  attackArc: 1.05,
  signatureCooldown: 7,
};
