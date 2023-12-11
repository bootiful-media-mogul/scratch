import { Ai } from '@/ai/ai'
import Mogul from '@/mogul'
import mitt from 'mitt'
import { cacheExchange, Client, fetchExchange } from '@urql/core'

export const graphqlClient = new Client({
  url: '/api/graphql',
  exchanges: [cacheExchange, fetchExchange]
})

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

////
export class Podcast {
  readonly title: string
  readonly id: number

  constructor(id: number, title: string) {
    this.id = id
    this.title = title
  }
}

class Podcasts {
  async delete(id: number) {
    const mutation = `
         mutation DeletePodcast ($id: ID ){ 
          deletePodcast(id: $id)  
         }
        `
    const result = await graphqlClient.mutation(mutation, {
      id: id
    })
    return (await result.data['deletePodcast']) as Number
  }
  async podcasts() {
    const q = `
        query {
          podcasts  { 
           id, title
          }
         }
        `
    const result = await graphqlClient.query(q, {})
    return (await result.data['podcasts']) as Array<Podcast>
  }

  async create(title: String): Promise<Podcast> {
    const mutation = `
         mutation CreatePodcast ($title: String){ 
          createPodcast(title: $title) { 
           id, title
          }
         }
        `
    const result = await graphqlClient.mutation(mutation, {
      title: title
    })
    console.log(JSON.stringify(result))
    return (await result.data['createPodcast']) as Podcast
  }
}

//======
// managed files

// todo
export class ManagedFile {
  readonly id: number

  constructor(id: number) {
    this.id = id
  }
}

export class ManagedFiles {
  async getManagedFileById(id: number): Promise<ManagedFile> {
    return new ManagedFile(id)
  }
}

export const ai = new Ai(graphqlClient)
export const mogul = new Mogul(graphqlClient)
export const events = mitt()
export const podcasts = new Podcasts()
export const managedFiles = new ManagedFiles()
