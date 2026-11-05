import type { Combatant, DamagePacket, DamageTag, DamageType } from "./combat/Damage";
import { CombatPipeline } from "./combat/pipeline";
import { SpatialHash } from "./collision/SpatialHash";
import { Rng } from "./rng";

export const STEP = 1 / 60;
export const VIEW_W = 320;
export const VIEW_H = 180;
export const WORLD_W = 640;
export const WORLD_H = 360;

export interface CombatProfile {
  id: string;
  name: string;
  maxHp: number;
  ad: number;
  ms: number;
  attackInterval: number;
  attackRange: number;
  attackArc: number;
  signatureCooldown: number;
}

export interface Intent {
  moveX: number;
  moveY: number;
  aimX: number;
  aimY: number;
  hasAim: boolean;
  skill: boolean;
}

export type EnemyKind = "brawler" | "thrower";

interface Body extends Combatant {
  x: number;
  y: number;
  radius: number;
  flash: number;
  facing: number;
}

interface Enemy extends Body {
  kind: EnemyKind;
  speed: number;
  attackRange: number;
  attackInterval: number;
  attackTimer: number;
  attackDamage: number;
  slowPct: number;
  slowUntil: number;
}

interface Projectile {
  x: number;
  y: number;
  vx: number;
  vy: number;
  damage: number;
  alive: boolean;
}

interface Dash {
  vx: number;
  vy: number;
  timeLeft: number;
  kind: "skill" | "passive";
  hit: Set<number>;
}

export interface OutputEvent {
  kind: "damage" | "impact" | "hit";
  x: number;
  y: number;
  amount?: number;
  crit?: boolean;
  strength?: 1 | 2 | 3;
}

export interface UnitView {
  id: number;
  x: number;
  y: number;
  radius: number;
  hp: number;
  maxHp: number;
  flash: boolean;
  facing: number;
  kind?: EnemyKind;
}

export interface Snapshot {
  time: number;
  player: UnitView & { skillCd: number; skillMax: number; alive: boolean };
  enemies: UnitView[];
  projectiles: { x: number; y: number }[];
  killed: number;
  total: number;
  events: OutputEvent[];
}

export function createWorld(options: { seed: number; profile: CombatProfile; enemyCount?: number }): World {
  return new World(options.seed, options.profile, options.enemyCount ?? 50);
}

export class World {
  readonly profile: CombatProfile;
  private readonly rng: Rng;
  private readonly units = new Map<number, Combatant>();
  private readonly pipeline: CombatPipeline;
  private readonly hash = new SpatialHash<Enemy>(32);
  private readonly nearby: Enemy[] = [];
  private readonly enemies: Enemy[] = [];
  private readonly projectiles: Projectile[] = [];
  private readonly output: OutputEvent[] = [];
  private readonly player: Body;
  private dash: Dash | null = null;
  private attackTimer = 0.2;
  private swing = 0;
  private skillCd = 0;
  private time = 0;
  private nextId = 2;
  private killed = 0;
  readonly total: number;

  constructor(seed: number, profile: CombatProfile, enemyCount: number) {
    this.profile = profile;
    this.rng = new Rng(seed);
    this.player = this.body(1, WORLD_W / 2, WORLD_H / 2, 7, profile.maxHp);
    this.units.set(1, this.player);
    this.pipeline = new CombatPipeline(this.units, this.rng);
    this.total = enemyCount;
    this.spawnRing(enemyCount);
  }

  step(dt: number, input: Intent): void {
    this.output.length = 0;
    this.time += dt;
    this.skillCd = Math.max(0, this.skillCd - dt);
    this.player.flash = Math.max(0, this.player.flash - dt);

const aim = this.aimVector(input);
      if (input.hasAim && aim.len > 0) this.player.facing = Math.atan2(aim.y, aim.x);
      else this.faceNearest();

    if (this.player.alive) {
      if (input.skill && this.skillCd <= 0 && !this.dash) this.castSignature(aim);
      this.rebuildHash();
      if (this.dash) this.advanceDash(dt);
      else this.movePlayer(dt, input);
      this.autoAttack(dt);
    } else {
      this.rebuildHash();
    }

    for (const enemy of this.enemies) {
      if (!enemy.alive) continue;
      enemy.flash = Math.max(0, enemy.flash - dt);
      this.think(enemy, dt);
    }
    this.advanceProjectiles(dt);
    this.separate();
    this.collectDead();
  }

  snapshot(): Snapshot {
    return {
      time: this.time,
      player: {
        id: this.player.id,
        x: this.player.x,
        y: this.player.y,
        radius: this.player.radius,
        hp: this.player.hp,
        maxHp: this.player.maxHp,
        flash: this.player.flash > 0,
        facing: this.player.facing,
        skillCd: this.skillCd,
        skillMax: this.profile.signatureCooldown,
        alive: this.player.alive,
      },
      enemies: this.enemies
        .filter((e) => e.alive)
        .map((e) => ({
          id: e.id,
          x: e.x,
          y: e.y,
          radius: e.radius,
          hp: e.hp,
          maxHp: e.maxHp,
          flash: e.flash > 0,
          facing: e.facing,
          kind: e.kind,
        })),
      projectiles: this.projectiles.filter((p) => p.alive).map((p) => ({ x: p.x, y: p.y })),
      killed: this.killed,
      total: this.total,
      events: this.output.slice(),
    };
  }

  private spawnRing(count: number): void {
    for (let i = 0; i < count; i++) {
      const angle = (i / count) * Math.PI * 2;
      const dist = 120 + this.rng.next() * 90;
      const kind: EnemyKind = i % 5 === 0 ? "thrower" : "brawler";
      const enemy = this.makeEnemy(kind, WORLD_W / 2 + Math.cos(angle) * dist, WORLD_H / 2 + Math.sin(angle) * dist);
      this.enemies.push(enemy);
      this.units.set(enemy.id, enemy);
    }
  }

  private makeEnemy(kind: EnemyKind, x: number, y: number): Enemy {
    const hp = kind === "brawler" ? 16 : 12;
    const body = this.body(this.nextId++, x, y, kind === "brawler" ? 6 : 5, hp);
    return {
      ...body,
      kind,
      speed: kind === "brawler" ? 34 : 26,
      attackRange: kind === "brawler" ? 16 : 110,
      attackInterval: kind === "brawler" ? 1.05 : 1.7,
      attackTimer: 0.4 + this.rng.next() * 0.6,
      attackDamage: kind === "brawler" ? 5 : 6,
      slowPct: 0,
      slowUntil: 0,
    };
  }

  private body(id: number, x: number, y: number, radius: number, hp: number): Body {
    return {
      id,
      x,
      y,
      radius,
      hp,
      maxHp: hp,
      armor: 0,
      mr: 0,
      dr: 0,
      ov: 0,
      crit: 0,
      critDmg: 1.5,
      shields: [],
      alive: true,
      flash: 0,
      facing: 0,
    };
  }

  private faceNearest(): void {
    let best = Infinity;
    for (const enemy of this.enemies) {
      if (!enemy.alive) continue;
      const dx = enemy.x - this.player.x;
      const dy = enemy.y - this.player.y;
      const dist = dx * dx + dy * dy;
      if (dist < best) {
        best = dist;
        this.player.facing = Math.atan2(dy, dx);
      }
    }
  }

  private movePlayer(dt: number, input: Intent): void {
    const len = Math.hypot(input.moveX, input.moveY);
    if (len === 0) return;
    const speed = this.profile.ms;
    this.player.x += (input.moveX / len) * speed * dt;
    this.player.y += (input.moveY / len) * speed * dt;
    this.clamp(this.player);
  }

  private aimVector(input: Intent): { x: number; y: number; len: number } {
    if (input.hasAim) {
      const x = input.aimX - this.player.x;
      const y = input.aimY - this.player.y;
      return { x, y, len: Math.hypot(x, y) };
    }
    return { x: Math.cos(this.player.facing), y: Math.sin(this.player.facing), len: 1 };
  }

  private castSignature(aim: { x: number; y: number; len: number }): void {
    const len = aim.len > 0 ? aim.len : 1;
    const distance = 72;
    const duration = 0.12;
    this.skillCd = this.profile.signatureCooldown;
    this.player.facing = Math.atan2(aim.y, aim.x);
    this.dash = {
      vx: (aim.x / len) * (distance / duration),
      vy: (aim.y / len) * (distance / duration),
      timeLeft: duration,
      kind: "skill",
      hit: new Set(),
    };
    this.output.push({ kind: "impact", x: this.player.x, y: this.player.y, strength: 2 });
  }

  private advanceDash(dt: number): void {
    const dash = this.dash;
    if (!dash) return;
    const step = Math.min(dt, dash.timeLeft);
    this.player.x += dash.vx * step;
    this.player.y += dash.vy * step;
    this.clamp(this.player);
    dash.timeLeft -= step;
    if (dash.kind === "skill") this.skillHits();
    if (dash.timeLeft <= 0) this.dash = null;
  }

  private skillHits(): void {
    const dash = this.dash;
    if (!dash) return;
    this.nearby.length = 0;
    this.hash.query(this.player.x, this.player.y, this.player.radius + 20, this.nearby);
    for (const enemy of this.nearby) {
      if (!enemy.alive || dash.hit.has(enemy.id)) continue;
      if (Math.hypot(enemy.x - this.player.x, enemy.y - this.player.y) > this.player.radius + enemy.radius + 6) continue;
      dash.hit.add(enemy.id);
      this.hurt(enemy, {
        amount: 20 + this.profile.ad * 0.7,
        type: "physical",
        tags: ["skill"],
        sourceId: "skill:short-dash",
      });
      enemy.slowPct = 0.3;
      enemy.slowUntil = this.time + 1;
    }
  }

  private autoAttack(dt: number): void {
    this.attackTimer -= dt;
    if (this.attackTimer > 0) return;
    const interval = Math.max(0.12, this.profile.attackInterval);
    this.attackTimer += interval;
    this.swing += 1;
    const empowered = this.swing % 4 === 0;
    let hits = 0;
    for (const enemy of this.enemies) {
      if (!enemy.alive) continue;
      const dx = enemy.x - this.player.x;
      const dy = enemy.y - this.player.y;
      const dist = Math.hypot(dx, dy);
      if (dist > this.profile.attackRange + enemy.radius) continue;
      const angle = Math.atan2(dy, dx);
      let delta = angle - this.player.facing;
      while (delta > Math.PI) delta -= Math.PI * 2;
      while (delta < -Math.PI) delta += Math.PI * 2;
      if (Math.abs(delta) > this.profile.attackArc) continue;
      hits += 1;
      this.hurt(enemy, {
        amount: this.profile.ad * (empowered ? 1.4 : 1),
        type: "physical",
        tags: ["attack"],
        sourceId: "skill:auto",
      });
    }
    if (hits > 0) this.output.push({ kind: "hit", x: this.player.x, y: this.player.y });
    if (empowered && !this.dash) {
      this.dash = {
        vx: Math.cos(this.player.facing) * (48 / 0.1),
        vy: Math.sin(this.player.facing) * (48 / 0.1),
        timeLeft: 0.1,
        kind: "passive",
        hit: new Set(),
      };
    }
  }

  private think(enemy: Enemy, dt: number): void {
    const dx = this.player.x - enemy.x;
    const dy = this.player.y - enemy.y;
    const dist = Math.hypot(dx, dy) || 1;
    const slow = this.time < enemy.slowUntil ? 1 - enemy.slowPct : 1;
    const speed = enemy.speed * slow;
    enemy.facing = Math.atan2(dy, dx);
    enemy.attackTimer -= dt;

    if (enemy.kind === "brawler") {
      if (dist > enemy.attackRange) {
        enemy.x += (dx / dist) * speed * dt;
        enemy.y += (dy / dist) * speed * dt;
      } else if (enemy.attackTimer <= 0 && this.player.alive) {
        enemy.attackTimer = enemy.attackInterval;
        this.hurt(this.player, {
          amount: enemy.attackDamage,
          type: "physical",
          tags: ["attack"],
          sourceId: "enemy:brawler",
          owner: enemy.id,
          target: this.player.id,
        });
      }
    } else {
      const desired = 100;
      const dir = dist > desired + 16 ? 1 : dist < desired - 16 ? -1 : 0;
      enemy.x += (dx / dist) * dir * speed * dt;
      enemy.y += (dy / dist) * dir * speed * dt;
      if (dist < enemy.attackRange && enemy.attackTimer <= 0 && this.player.alive) {
        enemy.attackTimer = enemy.attackInterval;
        const travel = 90;
        this.projectiles.push({
          x: enemy.x,
          y: enemy.y,
          vx: (dx / dist) * travel,
          vy: (dy / dist) * travel,
          damage: enemy.attackDamage,
          alive: true,
        });
      }
    }
    this.clamp(enemy);
  }

  private advanceProjectiles(dt: number): void {
    for (const rock of this.projectiles) {
      if (!rock.alive) continue;
      rock.x += rock.vx * dt;
      rock.y += rock.vy * dt;
      if (rock.x < 0 || rock.y < 0 || rock.x > WORLD_W || rock.y > WORLD_H) rock.alive = false;
      if (!this.player.alive) continue;
      if (Math.hypot(rock.x - this.player.x, rock.y - this.player.y) <= this.player.radius + 3) {
        rock.alive = false;
        this.hurt(this.player, {
          amount: rock.damage,
          type: "physical",
          tags: ["skill"],
          sourceId: "enemy:thrower",
          owner: 0,
          target: this.player.id,
        });
      }
    }
  }

  private separate(): void {
    this.rebuildHash();
    for (const enemy of this.enemies) {
      if (!enemy.alive) continue;
      this.nearby.length = 0;
      this.hash.query(enemy.x, enemy.y, 16, this.nearby);
      for (const other of this.nearby) {
        if (other.id <= enemy.id || !other.alive) continue;
        const dx = other.x - enemy.x;
        const dy = other.y - enemy.y;
        const dist = Math.hypot(dx, dy) || 0.001;
        const min = enemy.radius + other.radius;
        if (dist >= min) continue;
        const push = (min - dist) / 2;
        enemy.x -= (dx / dist) * push;
        enemy.y -= (dy / dist) * push;
        other.x += (dx / dist) * push;
        other.y += (dy / dist) * push;
      }
    }
  }

  private hurt(
    target: Body,
    hit: {
      amount: number;
      type: DamageType;
      tags: DamageTag[];
      sourceId: string;
      owner?: number;
      target?: number;
    },
  ): void {
    const packet: DamagePacket = {
      amount: hit.amount,
      type: hit.type,
      tags: hit.tags,
      sourceId: hit.sourceId,
      chain: [hit.sourceId],
      owner: hit.owner ?? this.player.id,
      target: hit.target ?? target.id,
      crit: false,
      procRatio: 1,
      depth: 0,
      dmgPct: 0,
    };
    const before = target.hp;
    this.pipeline.deal(packet);
    if (target.hp < before) {
      target.flash = 0.08;
      this.output.push({
        kind: "damage",
        x: target.x,
        y: target.y - target.radius,
        amount: Math.round(before - target.hp),
        crit: packet.crit,
      });
      if (target === this.player) this.output.push({ kind: "impact", x: target.x, y: target.y, strength: 1 });
    }
    if (!target.alive && target !== this.player) this.killed += 1;
  }

  private collectDead(): void {
    for (let i = this.projectiles.length - 1; i >= 0; i--) {
      if (!this.projectiles[i].alive) this.projectiles.splice(i, 1);
    }
  }

  private rebuildHash(): void {
    this.hash.clear();
    for (const enemy of this.enemies) if (enemy.alive) this.hash.insert(enemy);
  }

  private clamp(body: { x: number; y: number; radius: number }): void {
    body.x = Math.min(WORLD_W - body.radius, Math.max(body.radius, body.x));
    body.y = Math.min(WORLD_H - body.radius, Math.max(body.radius, body.y));
  }
}
