export function period(start: string, end: string) {
  const left = start.trim()
  const right = end.trim()
  if (!left && !right) return ''
  if (!right) return left
  if (!left) return right
  return `${left} - ${right}`
}

export function visibleBullets(bullets: string[]) {
  return bullets.map((item) => item.trim()).filter(Boolean)
}
