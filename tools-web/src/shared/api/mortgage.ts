import http from './http'

export type MortgageRepaymentMethod = 'EQUAL_INSTALLMENT' | 'EQUAL_PRINCIPAL'
export type MortgageProvidentTier = 'FIRST_HOME' | 'SECOND_HOME'

export interface MortgageDefaultsResponse {
  commercialAnnualRatePercent: number
  providentFundFirstHomeFiveYearOrBelowPercent: number
  providentFundFirstHomeFiveYearPlusPercent: number
  providentFundSecondHomeFiveYearOrBelowPercent: number
  providentFundSecondHomeFiveYearPlusPercent: number
  providentFundRateNote: string
  referencePeriod: string
  referenceNote: string
}

export interface MortgageCalculateRequest {
  years: number
  repaymentMethod: MortgageRepaymentMethod
  commercialPrincipal: number
  providentFundPrincipal: number
  commercialAnnualRatePercent?: number
  providentFundAnnualRatePercent?: number
  providentFundTier?: MortgageProvidentTier
  targetTotalInterestAmount?: number
  prepayWindowStartYear?: number
  prepayWindowEndYear?: number
}

export interface MortgageSummary {
  firstMonthPayment: number
  lastMonthPayment: number
  totalPayment: number
  totalInterest: number
}

export interface MortgageScheduleRow {
  monthIndex: number
  monthlyPayment: number
  principalPortion: number
  interestPortion: number
  remainingPrincipal: number
}

export interface MortgageCalculateResponse {
  combined: MortgageSummary
  combinedSchedule: MortgageScheduleRow[]
}

export function getMortgageDefaults() {
  return http.get<unknown, MortgageDefaultsResponse>('/mortgage/defaults')
}

export function calculateMortgage(payload: MortgageCalculateRequest) {
  return http.post<unknown, MortgageCalculateResponse>('/mortgage/calculate', payload)
}
