export const HAND_RANK_NAMES: Record<string, string> = {
  HIGH_CARD: '高牌',
  ONE_PAIR: '一对',
  TWO_PAIR: '两对',
  THREE_OF_A_KIND: '三条',
  STRAIGHT: '顺子',
  FLUSH: '同花',
  FULL_HOUSE: '葫芦',
  FOUR_OF_A_KIND: '四条',
  STRAIGHT_FLUSH: '同花顺',
  ROYAL_FLUSH: '皇家同花顺'
}

const SUIT_SYMBOL: Record<string, string> = {
  S: '♠',
  H: '♥',
  D: '♦',
  C: '♣'
}

const RANK_LABEL: Record<string, string> = {
  A: 'A',
  K: 'K',
  Q: 'Q',
  J: 'J',
  T: '10'
}

export interface ParsedCard {
  code: string
  rankLabel: string
  suitSymbol: string
  red: boolean
}

export function parseCard(code: string): ParsedCard {
  const rank = code[0]
  const suit = code[1]
  return {
    code,
    rankLabel: RANK_LABEL[rank] ?? rank,
    suitSymbol: SUIT_SYMBOL[suit] ?? suit,
    red: suit === 'H' || suit === 'D'
  }
}
