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

  async createPodcastEpisodeDraft(
    podcastId: number,
    title: String,
    description: String
  ): Promise<Podcast> {
    const mutation = `
         mutation CreatePodcastEpisodeDraft ($podcast: Float, $title: String, $description: String ){ 
          createPodcastEpisodeDraft( podcastId: $podcast, title: $title, description: $description) { 
           id 
          }
         }
        `
    console.log(podcastId + ':' + title + ':' + description)
    const result = await graphqlClient.mutation(mutation, {
      podcast: podcastId,
      title: title,
      description: description
    })
    return (await result.data['createPodcastEpisodeDraft']) as Podcast
  }

  async podcastById(podcastId: number): Promise<Podcast> {
    const q = `
       
            query GetPodcastById( $id: ID){
                podcastById ( id : $id) { 
                    id,
                    title
                }
            }
        `
    const result = await graphqlClient.query(q, { id: podcastId })
    return (await result.data['podcastById']) as Podcast
  }
}

//======
// managed files

// todo
export class ManagedFile {
  readonly id: number
  readonly bucket: string
  readonly folder: string
  readonly filename: string
  readonly size: number
  readonly written: boolean

  constructor(
    id: number,
    bucket: string,
    folder: string,
    filename: string,
    size: number,
    written: boolean
  ) {
    this.id = id
    this.bucket = bucket
    this.folder = folder
    this.filename = filename
    this.written = written
    this.size = size
  }
}

export class ManagedFiles {
  async getManagedFileById(id: number): Promise<ManagedFile> {
    const q = `
        query {
          managedFileById()  { 
           id, title
          }
         }
        `
    const result = await graphqlClient.query(q, {})
    return (await result.data['managedFileById']) as ManagedFile
  }
}

export const ai = new Ai(graphqlClient)
export const mogul = new Mogul(graphqlClient)
export const events = mitt()
export const podcasts = new Podcasts()
export const managedFiles = new ManagedFiles()
