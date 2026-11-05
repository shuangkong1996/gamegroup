import { invitation } from '../data/invitation'

export interface RsvpPayload {
  name: string
  attending: boolean
  guestCount: number
}

export async function submitRsvp(payload: RsvpPayload): Promise<void> {
  const response = await fetch(`/api/v1/wedding/invitations/${invitation.slug}/rsvps`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })

  if (!response.ok) {
    throw new Error('暂时无法提交，请稍后再试。')
  }
}
