export type HsAlgorithm = 'HS256' | 'HS384' | 'HS512'

export interface JwtParts {
  header: Record<string, unknown>
  payload: Record<string, unknown>
  signature: string
  headerRaw: string
  payloadRaw: string
  signingInput: string
}

export interface TimeFieldInfo {
  key: string
  value: number
  text: string
  expired?: boolean
  remainingSeconds?: number
}

const textEncoder = new TextEncoder()
const textDecoder = new TextDecoder()

const algoMap: Record<HsAlgorithm, string> = {
  HS256: 'SHA-256',
  HS384: 'SHA-384',
  HS512: 'SHA-512'
}

export function isHsAlgorithm(value: unknown): value is HsAlgorithm {
  return value === 'HS256' || value === 'HS384' || value === 'HS512'
}

export function base64UrlEncode(bytes: ArrayBuffer | Uint8Array | string): string {
  const data =
    typeof bytes === 'string'
      ? textEncoder.encode(bytes)
      : bytes instanceof Uint8Array
        ? bytes
        : new Uint8Array(bytes)
  let binary = ''
  data.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

export function base64UrlDecode(value: string): Uint8Array {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized + '='.repeat((4 - (normalized.length % 4 || 4)) % 4)
  const binary = atob(padded)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}

export function base64UrlDecodeToString(value: string): string {
  return textDecoder.decode(base64UrlDecode(value))
}

export function parseJwt(token: string): JwtParts {
  const cleaned = token.trim().replace(/^Bearer\s+/i, '')
  const parts = cleaned.split('.')
  if (parts.length !== 3 || parts.some((part) => !part)) {
    throw new Error('Token 格式无效，应为 header.payload.signature')
  }

  const [headerRaw, payloadRaw, signature] = parts
  let header: Record<string, unknown>
  let payload: Record<string, unknown>

  try {
    header = JSON.parse(base64UrlDecodeToString(headerRaw)) as Record<string, unknown>
  } catch {
    throw new Error('Header 解码失败')
  }

  try {
    payload = JSON.parse(base64UrlDecodeToString(payloadRaw)) as Record<string, unknown>
  } catch {
    throw new Error('Payload 解码失败')
  }

  return {
    header,
    payload,
    signature,
    headerRaw,
    payloadRaw,
    signingInput: `${headerRaw}.${payloadRaw}`
  }
}

export function formatJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

export function extractTimeFields(payload: Record<string, unknown>): TimeFieldInfo[] {
  const now = Math.floor(Date.now() / 1000)
  return (['iat', 'nbf', 'exp'] as const)
    .map((key) => {
      const raw = payload[key]
      if (typeof raw !== 'number' || !Number.isFinite(raw)) {
        return null
      }
      const info: TimeFieldInfo = {
        key,
        value: raw,
        text: new Date(raw * 1000).toLocaleString('zh-CN', { hour12: false })
      }
      if (key === 'exp') {
        info.expired = raw < now
        info.remainingSeconds = raw - now
      }
      return info
    })
    .filter((item): item is TimeFieldInfo => item !== null)
}

export function formatDuration(seconds: number): string {
  const abs = Math.abs(seconds)
  const days = Math.floor(abs / 86400)
  const hours = Math.floor((abs % 86400) / 3600)
  const minutes = Math.floor((abs % 3600) / 60)
  const secs = abs % 60
  const parts = [
    days ? `${days}天` : '',
    hours ? `${hours}小时` : '',
    minutes ? `${minutes}分` : '',
    !days && !hours ? `${secs}秒` : ''
  ].filter(Boolean)
  return parts.join('') || '0秒'
}

async function importHmacKey(secret: string, algorithm: HsAlgorithm) {
  return crypto.subtle.importKey(
    'raw',
    textEncoder.encode(secret),
    {
      name: 'HMAC',
      hash: algoMap[algorithm]
    },
    false,
    ['sign', 'verify']
  )
}

export async function signHsJwt(
  header: Record<string, unknown>,
  payload: Record<string, unknown>,
  secret: string,
  algorithm: HsAlgorithm = 'HS256'
): Promise<string> {
  if (!secret) {
    throw new Error('Secret 不能为空')
  }
  const normalizedHeader = {
    ...header,
    alg: algorithm,
    typ: header.typ ?? 'JWT'
  }
  const compactHeader = base64UrlEncode(JSON.stringify(normalizedHeader))
  const compactPayload = base64UrlEncode(JSON.stringify(payload))
  const signingInput = `${compactHeader}.${compactPayload}`
  const key = await importHmacKey(secret, algorithm)
  const signature = await crypto.subtle.sign('HMAC', key, textEncoder.encode(signingInput))
  return `${signingInput}.${base64UrlEncode(signature)}`
}

export async function verifyHsSignature(token: string, secret: string): Promise<{
  valid: boolean
  algorithm: HsAlgorithm
  message: string
}> {
  if (!secret) {
    throw new Error('请输入用于校验的 Secret')
  }

  const parts = parseJwt(token)
  const algorithm = parts.header.alg
  if (!isHsAlgorithm(algorithm)) {
    throw new Error(`当前仅支持 HS256/HS384/HS512，检测到算法：${String(algorithm ?? '未知')}`)
  }

  const key = await importHmacKey(secret, algorithm)
  const signatureBytes = base64UrlDecode(parts.signature)
  // Copy into a fresh ArrayBuffer-backed view for Web Crypto typing
  const signatureBuffer = new Uint8Array(signatureBytes)
  const valid = await crypto.subtle.verify(
    'HMAC',
    key,
    signatureBuffer,
    textEncoder.encode(parts.signingInput)
  )

  return {
    valid,
    algorithm,
    message: valid ? '签名校验通过' : '签名校验失败，Secret 或 Token 不匹配'
  }
}

export function createDefaultHeader(algorithm: HsAlgorithm = 'HS256') {
  return {
    alg: algorithm,
    typ: 'JWT'
  }
}

export function createDefaultPayload(hours = 1) {
  const now = Math.floor(Date.now() / 1000)
  return {
    iss: 'tools-web',
    sub: 'demo',
    iat: now,
    exp: now + hours * 3600
  }
}

export function randomSecret(min = 20, max = 40): string {
  const length = Math.floor(Math.random() * (max - min + 1)) + min
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+'
  const bytes = new Uint32Array(length)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (value) => alphabet[value % alphabet.length]).join('')
}
