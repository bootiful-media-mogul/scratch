import { AiClient } from '@/aiClient'
import MogulClient from '@/mogulClient'
import mitt from 'mitt'

export const ai = new AiClient()
export const mogul = new MogulClient()
export const events = mitt()

export class AiWorkshopEvent {
  readonly text: string
  readonly ref: any

  constructor(text: string, ref: any) {
    this.text = text
    this.ref = ref
  }
}

export function workshopInAi(ref: any, text: string) {
  events.emit('ai-workshop-event', new AiWorkshopEvent(text, ref))
}
