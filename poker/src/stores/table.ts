import { defineStore } from 'pinia'
import { pokerSocket } from '@/shared/ws/socket'
import { getRoom, dissolveRoom } from '@/shared/api/poker'
import { router } from '@/router'

let snapshotHandler: ((data: TableSnapshot) => void) | null = null
let errorHandler: ((data: { message: string }) => void) | null = null
let closedHandler: (() => void) | null = null

export interface PlayerView {
  seat: number
  userId: number
  nickname: string
  avatarUrl: string
  chip: string
  holeCards?: string[]
  roundBet?: string
  totalBet?: string
  folded?: boolean
  allIn?: boolean
  isDealer?: boolean
}

export interface HandResultView {
  pot: string
  winners: number[]
  chipDelta: Record<number, string>
  revealedHoleCards: Record<number, string[]>
  bestHands: Record<number, string>
}

export interface TableSnapshot {  roomId: number
  roomNo: string
  name: string
  tableSize: number
  smallBlind: string
  bigBlind: string
  status: string
  handNo: number
  dealerSeat: number | null
  phase: string
  board: string[]
  pot: string
  currentBet: string
  minRaise: string
  currentActor: number | null
  callAmount: string
  actionDeadline: number | null
  isOwner: boolean
  mySeat: number | null
  players: PlayerView[]
  lastHand: HandResultView | null
}

export const useTableStore = defineStore('table', {
  state: () => ({
    roomId: null as number | null,
    snapshot: null as TableSnapshot | null,
    connected: false,
    error: null as string | null,
    pending: false
  }),

  getters: {
    mySeat: (state) => state.snapshot?.mySeat ?? null,
    me(state): PlayerView | null {
      if (!state.snapshot) return null
      return state.snapshot.players.find((p) => p.seat === state.snapshot!.mySeat) ?? null
    },
    isMyTurn(state): boolean {
      const s = state.snapshot
      return !!s && s.currentActor != null && s.currentActor === s.mySeat
    },
    playersBySeat(state): (PlayerView | null)[] {
      if (!state.snapshot) return []
      const arr: (PlayerView | null)[] = new Array(state.snapshot.tableSize).fill(null)
      state.snapshot.players.forEach((p) => {
        arr[p.seat] = p
      })
      return arr
    }
  },

  actions: {
    onSnapshot(data: TableSnapshot) {
      this.snapshot = data
      this.error = null
      this.pending = false
    },

    onError(data: { message: string }) {
      this.error = data?.message || '操作失败'
      this.pending = false
    },

    async enterRoom(roomId: number | string) {
      const id = Number(roomId)
      this.roomId = id
      this.snapshot = null
      this.error = null
      this.pending = false

      snapshotHandler = (data: TableSnapshot) => this.onSnapshot(data)
      errorHandler = (data: { message: string }) => this.onError(data)
      closedHandler = () => this.onRoomClosed()
      pokerSocket.on<TableSnapshot>('TABLE_SNAPSHOT', snapshotHandler!)
      pokerSocket.on<{ message: string }>('ERROR', errorHandler!)
      pokerSocket.on('ROOM_CLOSED', closedHandler!)
      pokerSocket.connect(id)
    },

    leaveRoom() {
      pokerSocket.off('TABLE_SNAPSHOT', snapshotHandler!)
      pokerSocket.off('ERROR', errorHandler!)
      pokerSocket.off('ROOM_CLOSED', closedHandler!)
      pokerSocket.disconnect()
      this.roomId = null
      this.snapshot = null
    },

    onRoomClosed() {
      this.leaveRoom()
      router.replace({ path: '/lobby', query: { msg: 'room-closed' } })
    },

    sitDown(seat: number, buyIn: number) {
      this.pending = true
      this.error = null
      pokerSocket.send({ type: 'SIT_DOWN', roomId: this.roomId, seat, buyIn })
    },

    standUp() {
      this.pending = true
      this.error = null
      pokerSocket.send({ type: 'STAND_UP', roomId: this.roomId })
    },

    startGame() {
      this.pending = true
      this.error = null
      pokerSocket.send({ type: 'START_GAME', roomId: this.roomId })
    },

    act(action: 'FOLD' | 'CHECK' | 'CALL' | 'BET' | 'RAISE' | 'ALL_IN', amount?: number) {
      this.pending = true
      this.error = null
      pokerSocket.send({
        type: 'ACTION',
        roomId: this.roomId,
        action,
        amount: amount ?? null
      })
    },

    clearError() {
      this.error = null
    },

    clearLastHand() {
      if (this.snapshot) {
        this.snapshot.lastHand = null
      }
    },

    async dissolveRoom() {
      if (!this.roomId) return
      await dissolveRoom(this.roomId)
      this.leaveRoom()
    },

    async fetchRoomInfo() {
      if (!this.roomId) return null
      return getRoom(this.roomId)
    }
  }
})
