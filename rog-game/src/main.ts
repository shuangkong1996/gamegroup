import Phaser from "phaser";
import { unlockAudio } from "./audio/sfx";
import { bladeguard } from "./content/bladeguard";
import { KeyboardInput } from "./input/KeyboardInput";
import { VIEW_H, VIEW_W, createWorld } from "./sim/World";
import "./ui/app.css";
import { BattleScene } from "./view/BattleScene";
import { fitInteger } from "./view/viewport";

const worldEl = document.querySelector<HTMLElement>("#world");
const hud = document.querySelector<HTMLElement>("#hud");
if (!worldEl || !hud) throw new Error("页面结构缺失");

fitInteger(worldEl, VIEW_W, VIEW_H);
window.addEventListener("resize", () => fitInteger(worldEl, VIEW_W, VIEW_H));

const input = new KeyboardInput();
input.attach(window);
window.addEventListener("pointerdown", () => unlockAudio());
window.addEventListener("keydown", () => unlockAudio());

const params = new URLSearchParams(location.search);
const seed = Number(params.get("seed") ?? Date.now() % 1_000_000);
const world = createWorld({ seed, profile: bladeguard });

new Phaser.Game({
  type: Phaser.WEBGL,
  parent: worldEl,
  width: VIEW_W,
  height: VIEW_H,
  pixelArt: true,
  roundPixels: true,
  backgroundColor: "#100e1c",
  audio: { noAudio: true },
  banner: false,
  fps: { smoothStep: false },
  scale: { mode: Phaser.Scale.NONE },
  input: { keyboard: false, mouse: false, touch: false, gamepad: false },
  scene: new BattleScene(world, input, hud),
});
