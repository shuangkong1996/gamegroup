import type { Intent } from "../sim/World";

export class KeyboardInput {
  private readonly keys = new Set<string>();
  private skillQueued = false;
  private aimX = 0;
  private aimY = 0;
  private hasAim = false;
  private detach = () => {};

  attach(target: Window): void {
    const down = (event: KeyboardEvent) => {
      this.keys.add(event.code);
      if (event.code === "KeyQ" || event.code === "Space") {
        this.skillQueued = true;
        event.preventDefault();
      }
    };
    const up = (event: KeyboardEvent) => this.keys.delete(event.code);
    target.addEventListener("keydown", down);
    target.addEventListener("keyup", up);
    this.detach = () => {
      target.removeEventListener("keydown", down);
      target.removeEventListener("keyup", up);
    };
  }

  setAim(x: number, y: number): void {
    this.aimX = x;
    this.aimY = y;
    this.hasAim = true;
  }

  pull(): Intent {
    const skill = this.skillQueued;
    this.skillQueued = false;
    let moveX = 0;
    let moveY = 0;
    if (this.keys.has("KeyA") || this.keys.has("ArrowLeft")) moveX -= 1;
    if (this.keys.has("KeyD") || this.keys.has("ArrowRight")) moveX += 1;
    if (this.keys.has("KeyW") || this.keys.has("ArrowUp")) moveY -= 1;
    if (this.keys.has("KeyS") || this.keys.has("ArrowDown")) moveY += 1;
    return { moveX, moveY, aimX: this.aimX, aimY: this.aimY, hasAim: this.hasAim, skill };
  }

  destroy(): void {
    this.detach();
  }
}
