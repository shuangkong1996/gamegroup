export interface PokerPlayerDTO {
  seat: number
  userId: number
  nickname: string
  avatarUrl: string
  chip: string
}

export interface PokerRoomDTO {
  id: number
  roomNo: string
  name: string
  ownerId: number
  tableSize: number
  smallBlind: string
  bigBlind: string
  buyInMin: string
  buyInMax: string
  thinkSeconds: number
  status: string
  handNo: number
  currentPlayers: number
  players: PokerPlayerDTO[]
}
