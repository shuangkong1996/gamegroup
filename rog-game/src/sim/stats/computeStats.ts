export type StatKey = "ad" | "hp" | "as" | "ms" | "armor" | "mr" | "dr" | "crit" | "critDmg" | "ov" | "dmgPct";

export interface StatMod {
  stat: StatKey;
  flat?: number;
  pct?: number;
}

export interface StatContext {
  gold: number;
}

export interface StatLayer {
  layer: string;
  sourceId: string;
  mods: StatMod[] | ((ctx: StatContext) => StatMod[]);
}

export interface StatSource {
  layer: string;
  sourceId: string;
  flat: number;
  pct: number;
}

export interface ComputedStats {
  values: Record<StatKey, number>;
  sources: Record<StatKey, StatSource[]>;
}

const KEYS: StatKey[] = ["ad", "hp", "as", "ms", "armor", "mr", "dr", "crit", "critDmg", "ov", "dmgPct"];

/** 先加平值，再乘百分比。金币相关的层在 ctx.gold 变化后重新计算即可。 */
export function computeStats(layers: StatLayer[], ctx: StatContext): ComputedStats {
  const flat: Record<StatKey, number> = empty();
  const pct: Record<StatKey, number> = empty();
  const sources = emptySources();

  for (const layer of layers) {
    const mods = typeof layer.mods === "function" ? layer.mods(ctx) : layer.mods;
    for (const mod of mods) {
      const f = mod.flat ?? 0;
      const p = mod.pct ?? 0;
      flat[mod.stat] += f;
      pct[mod.stat] += p;
      sources[mod.stat].push({ layer: layer.layer, sourceId: layer.sourceId, flat: f, pct: p });
    }
  }

  const values = empty();
  for (const key of KEYS) values[key] = (flat[key]) * (1 + pct[key]);
  return { values, sources };
}

function empty(): Record<StatKey, number> {
  return { ad: 0, hp: 0, as: 0, ms: 0, armor: 0, mr: 0, dr: 0, crit: 0, critDmg: 0, ov: 0, dmgPct: 0 };
}

function emptySources(): Record<StatKey, StatSource[]> {
  return { ad: [], hp: [], as: [], ms: [], armor: [], mr: [], dr: [], crit: [], critDmg: [], ov: [], dmgPct: [] };
}
