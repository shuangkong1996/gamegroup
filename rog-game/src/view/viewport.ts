export function fitInteger(el: HTMLElement, width: number, height: number): number {
  const scale = Math.max(1, Math.floor(Math.min(window.innerWidth / width, window.innerHeight / height)));
  const cssW = width * scale;
  const cssH = height * scale;
  el.style.width = `${cssW}px`;
  el.style.height = `${cssH}px`;
  el.style.left = `${Math.floor((window.innerWidth - cssW) / 2)}px`;
  el.style.top = `${Math.floor((window.innerHeight - cssH) / 2)}px`;
  return scale;
}
