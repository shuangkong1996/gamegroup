import Phaser from "phaser";
import { playHit, playSkill } from "../audio/sfx";
import { STEP, VIEW_H, VIEW_W, WORLD_H, WORLD_W, type OutputEvent, type Snapshot, type World } from "../sim/World";
import type { KeyboardInput } from "../input/KeyboardInput";

interface Floater {
  label: Phaser.GameObjects.Text;
  life: number;
  x: number;
  y: number;
}

export class BattleScene extends Phaser.Scene {
  private g!: Phaser.GameObjects.Graphics;
  private acc = 0;
  private shake = 0;
  private shakeCd = 0;
  private readonly floaters: Floater[] = [];
  private latest: Snapshot | null = null;

  constructor(
    private readonly world: World,
    private readonly controls: KeyboardInput,
    private readonly hud: HTMLElement,
  ) {
    super("battle");
  }

  create(): void {
    this.g = this.add.graphics();
    this.cameras.main.setBackgroundColor("#100e1c");
    const canvas = this.game.canvas;
    canvas.style.imageRendering = "pixelated";
    window.addEventListener("pointermove", (event) => {
      const rect = canvas.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) return;
      const cam = this.cameras.main;
      const x = ((event.clientX - rect.left) / rect.width) * cam.width + cam.scrollX;
      const y = ((event.clientY - rect.top) / rect.height) * cam.height + cam.scrollY;
      this.controls.setAim(x, y);
    });
  }

  update(_time: number, delta: number): void {
    if (document.hidden) {
      this.acc = 0;
      return;
    }
    this.acc += Math.min(delta / 1000, 0.25);
    let steps = 0;
    while (this.acc >= STEP && steps < 5) {
      this.world.step(STEP, this.controls.pull());
      const snap = this.world.snapshot();
      this.consume(snap);
      this.latest = snap;
      this.acc -= STEP;
      steps += 1;
    }
    if (this.latest) this.draw(this.latest);
  }

  private consume(snap: Snapshot): void {
    for (const event of snap.events) {
      if (event.kind === "damage" && event.amount) this.spawnFloater(event);
      if (event.kind === "hit") playHit(Boolean(event.crit));
      if (event.kind === "impact") {
        playSkill();
        if (this.shakeCd <= 0) {
          this.shake = 0.08;
          this.shakeCd = 0.12;
        }
      }
    }
  }

  private spawnFloater(event: OutputEvent): void {
    const active = this.floaters.filter((f) => f.life > 0).length;
    if (active >= 60) return;
    const label = this.add.text(event.x, event.y, String(event.amount), {
      fontFamily: "ui-monospace, monospace",
      fontSize: event.crit ? "10px" : "8px",
      color: event.crit ? "#fff4d6" : "#ffd7bf",
    });
    label.setOrigin(0.5, 1);
    this.floaters.push({ label, life: event.crit ? 0.35 : 0.5, x: event.x, y: event.y });
  }

  private draw(snap: Snapshot): void {
    this.shake = Math.max(0, this.shake - STEP);
    this.shakeCd = Math.max(0, this.shakeCd - STEP);
    const cam = this.cameras.main;
    const ox = this.shake > 0 ? (Math.sin(snap.time * 80) > 0 ? 1 : -1) : 0;
    cam.setScroll(
      clamp(snap.player.x - VIEW_W / 2, 0, WORLD_W - VIEW_W) + ox,
      clamp(snap.player.y - VIEW_H / 2, 0, WORLD_H - VIEW_H),
    );

    const g = this.g;
    g.clear();
    g.fillStyle(0x171427, 1);
    g.fillRect(0, 0, WORLD_W, WORLD_H);
    g.lineStyle(1, 0x2a2547, 1);
    for (let x = 0; x <= WORLD_W; x += 16) g.lineBetween(x, 0, x, WORLD_H);
    for (let y = 0; y <= WORLD_H; y += 16) g.lineBetween(0, y, WORLD_W, y);

    for (const enemy of snap.enemies) {
      g.fillStyle(enemy.flash ? 0xffffff : enemy.kind === "thrower" ? 0xb49cff : 0xff8a6a, 1);
      g.fillRect(enemy.x - enemy.radius, enemy.y - enemy.radius, enemy.radius * 2, enemy.radius * 2);
    }
    for (const rock of snap.projectiles) {
      g.fillStyle(0xffe45e, 1);
      g.fillRect(rock.x - 2, rock.y - 2, 4, 4);
    }

    const p = snap.player;
    g.fillStyle(p.flash ? 0xffffff : 0x4ee8d4, 1);
    g.fillRect(p.x - 5, p.y - 7, 10, 14);
    g.fillStyle(0xffc94d, 1);
    g.fillRect(p.x + Math.cos(p.facing) * 8 - 1, p.y + Math.sin(p.facing) * 8 - 1, 3, 3);

    for (const floater of this.floaters) {
      floater.life -= STEP;
      floater.y -= 18 * STEP;
      floater.label.setPosition(floater.x, floater.y);
      floater.label.setAlpha(Math.max(0, floater.life * 2));
      if (floater.life <= 0) floater.label.destroy();
    }
    for (let i = this.floaters.length - 1; i >= 0; i--) {
      if (this.floaters[i].life <= 0) this.floaters.splice(i, 1);
    }

    const cd = p.skillCd <= 0 ? "就绪" : `${p.skillCd.toFixed(1)}s`;
    const banner = !p.alive ? "倒下了" : snap.killed >= snap.total ? "这一波清完了" : `剩余 ${snap.total - snap.killed}`;
    this.hud.innerHTML = `<b>${this.world.profile.name}</b><br>生命 ${Math.ceil(p.hp)} / ${p.maxHp}<br>短折 ${cd}<br>${banner}<br><span class="dim">WASD 移动 · 鼠标瞄准 · Q 短折</span>`;
  }
}

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n));
}
