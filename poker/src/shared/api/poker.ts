import { del, get, post } from './http'
import type { PokerRoomDTO } from './poker-types'

export interface PokerStatsDTO {
  userId: number
  handsPlayed: number
  handsWon: number
  winRate: string
  totalWin: string
  totalBet: string
}

export interface CreateRoomParams {
  name: string
  tableSize: number
  smallBlind: number
  bigBlind: number
  buyInMin: number
  buyInMax: number
  thinkSeconds?: number
}

export function createRoom(params: CreateRoomParams) {
  return post<PokerRoomDTO>('/poker/rooms', params)
}

export function listRooms() {
  return get<PokerRoomDTO[]>('/poker/rooms')
}

export function joinRoom(roomNo: string) {
  return post<PokerRoomDTO>('/poker/rooms/join', { roomNo })
}

export function getRoom(roomId: number | string) {
  return get<PokerRoomDTO>(`/poker/rooms/${roomId}`)
}

export function dissolveRoom(roomId: number | string) {
  return del<void>(`/poker/rooms/${roomId}`)
}

export function fetchStats() {
  return get<PokerStatsDTO>('/poker/stats/me')
}
