export const TOKEN_KEY = 'token'

export function setSessionToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
}

export function hasValidSessionToken() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return false
  try {
    const payload = token.split('.')[1]
    if (!payload) return false
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decodedPayload = atob(normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '='))
    const exp = JSON.parse(decodedPayload).exp
    return typeof exp === 'number' && exp * 1000 > Date.now()
  } catch {
    return false
  }
}
