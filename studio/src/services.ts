import { AiClient } from '@/aiClient'
import MogulClient from '@/mogulClient'
import mitt from 'mitt'
import { cacheExchange, Client, fetchExchange } from '@urql/core'

export const graphqlClient = new Client({
  url: '/api/graphql',
  exchanges: [cacheExchange, fetchExchange]
})
export const ai = new AiClient(graphqlClient)
export const mogul = new MogulClient(graphqlClient)
export const events = mitt()

export enum AiWorkshopReplyEventType {
  TEXT,
  IMAGE
}

export class AiWorkshopReplyEvent {
  readonly text: string
  readonly type: AiWorkshopReplyEventType

  constructor(text: string, type: AiWorkshopReplyEventType) {
    this.text = text
    this.type = type
  }
}

export class AiWorkshopRequestEvent {
  readonly text: string

  readonly callback: (arg0: AiWorkshopReplyEvent) => void

  constructor(text: string, callback: (arg0: AiWorkshopReplyEvent) => void) {
    this.text = text
    this.callback = callback
  }
}

export function workshopInAi(callback: (e: AiWorkshopReplyEvent) => void, text: string) {
  events.emit('ai-workshop-event', new AiWorkshopRequestEvent(text, callback))
}