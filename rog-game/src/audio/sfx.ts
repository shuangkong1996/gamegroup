let context: AudioContext | null = null;

export function unlockAudio(): void {
  if (!context) context = new AudioContext();
  if (context.state === "suspended") void context.resume();
}

export function playHit(crit = false): void {
  if (!context || context.state !== "running") return;
  const now = context.currentTime;
  const osc = context.createOscillator();
  const gain = context.createGain();
  osc.type = "sine";
  osc.frequency.setValueAtTime(crit ? 320 : 210, now);
  osc.frequency.exponentialRampToValueAtTime(90, now + 0.06);
  gain.gain.setValueAtTime(0.06, now);
  gain.gain.exponentialRampToValueAtTime(0.001, now + 0.07);
  osc.connect(gain);
  gain.connect(context.destination);
  osc.start(now);
  osc.stop(now + 0.08);
}

export function playSkill(): void {
  if (!context || context.state !== "running") return;
  const now = context.currentTime;
  const osc = context.createOscillator();
  const gain = context.createGain();
  osc.type = "triangle";
  osc.frequency.setValueAtTime(140, now);
  osc.frequency.exponentialRampToValueAtTime(420, now + 0.09);
  gain.gain.setValueAtTime(0.05, now);
  gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);
  osc.connect(gain);
  gain.connect(context.destination);
  osc.start(now);
  osc.stop(now + 0.13);
}
