import { Ai } from '@/ai/ai'
import Mogul from '@/mogul'
import mitt from 'mitt'
import { Client, errorExchange, fetchExchange } from '@urql/core'
import router from '@/index'

export const graphqlClient = new Client({
  url: '/api/graphql',
  exchanges: [
    fetchExchange,
    errorExchange({
      onError: async (error) => {
        if (error) {
          console.error('got an error! ' + JSON.stringify(error))
          events.emit('unauthorized', error)
          await router.replace('/')
        }
      }
    })
  ]
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

export function previewManagedFile(managedFileId: number) {
  console.log('launching previewManagedFile for ' + managedFileId)
  events.emit('preview-managed-file-event', managedFileId)
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
  async publishPodcastEpisode(episodeId: number, pluginName: string): Promise<boolean> {
    const mutation = ` 
          mutation PublishPodcastEpisode  ($episode: ID, $pluginName: String ){ 
            publishPodcastEpisode ( episodeId: $episode,  pluginName: $pluginName ) 
          }
        `
    await graphqlClient.mutation(mutation, {
      episode: episodeId,
      pluginName: pluginName
    })
    return true
  }

  async updatePodcastEpisode(
    episodeId: number,
    title: string,
    description: string
  ): Promise<Episode> {
    const mutation = `
         mutation UpdatePodcastEpisode  ($episode: ID, $title: String, $description: String ){ 
          updatePodcastEpisode ( episodeId: $episode, title: $title, description: $description) { 
           availablePlugins,   created, id , title, description, complete, graphic { id  }, interview { id }, introduction { id }
          }
         }
        `
    console.log(episodeId + ':' + title + ':' + description)
    const result = await graphqlClient.mutation(mutation, {
      episode: episodeId,
      title: title,
      description: description
    })

    return (await result.data['updatePodcastEpisode']) as Episode
  }

  async podcastEpisodeById(id: number): Promise<Episode> {
    const q = `
           query GetPodcastEpisode ( $id: ID){
                podcastEpisodeById ( id : $id) {
                availablePlugins,   created,   id , title, description, complete,  graphic { id  }, interview { id }, introduction { id }
                }
        }
        `
    const res = await graphqlClient.query(q, { id: id })

    return (await res.data['podcastEpisodeById']) as Episode
  }

  async create(title: string): Promise<Podcast> {
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
    return (await result.data['createPodcast']) as Podcast
  }

  async podcastEpisodes(podcastId: number): Promise<Array<Episode>> {
    const q = `
           query GetPodcastEpisodesByPodcast( $podcastId: ID){
                podcastEpisodesByPodcast ( podcastId : $podcastId) {
                 availablePlugins,  created, id , title, description, complete, graphic { id  }, interview { id }, introduction { id }
                }
        }
        `
    const res = await graphqlClient.query(q, { podcastId: podcastId })

    return (await res.data['podcastEpisodesByPodcast']) as Array<Episode>
  }

  async deleteEpisode(id: number) {
    const mutation = `
         mutation DeletePodcastEpisode ($id: ID ){ 
          deletePodcastEpisode(id: $id)  
         }
        `
    const result = await graphqlClient.mutation(mutation, {
      id: id
    })
    return (await result.data['deletePodcastEpisode']) as Number
  }

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
    title: string,
    description: string
  ): Promise<Episode> {
    const mutation = `
         mutation CreatePodcastEpisodeDraft ($podcast: ID, $title: String, $description: String ){ 
          createPodcastEpisodeDraft( podcastId: $podcast, title: $title, description: $description) { 
            availablePlugins,    created,   id , title, description, complete,  graphic { id  }, interview { id }, introduction { id }
          }
         }
        `
    console.log(podcastId + ':' + title + ':' + description)
    const result = await graphqlClient.mutation(mutation, {
      podcast: podcastId,
      title: title,
      description: description
    })

    return (await result.data['createPodcastEpisodeDraft']) as Episode
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

// settings
export class Setting {
  name: string
  valid: boolean
  value: string

  constructor(name: string, valid: boolean, value: string) {
    this.name = name
    this.valid = valid
    this.value = value
  }
}

export class SettingsPage {
  valid: boolean
  category: string
  settings: Array<Setting>

  constructor(valid: boolean, category: string, settings: Array<Setting>) {
    this.valid = valid
    this.category = category
    this.settings = settings
  }
}

// settings

export class ManagedFile {
  id: number
  bucket: string
  folder: string
  filename: string
  size: number
  written: boolean
  contentType: string

  constructor(
    id: number,
    bucket: string,
    folder: string,
    filename: string,
    size: number,
    written: boolean,
    contentType: string
  ) {
    this.id = id
    this.bucket = bucket
    this.folder = folder
    this.filename = filename
    this.written = written
    this.size = size
    this.contentType = contentType
  }
}

export class Episode {
  availablePlugins: Array<string>
  id: number
  title: string
  description: string
  graphic: ManagedFile
  interview: ManagedFile
  introduction: ManagedFile
  complete: boolean = false
  created: number = 0

  constructor(
    id: number,
    title: string,
    description: string,
    graphic: ManagedFile,
    interview: ManagedFile,
    introduction: ManagedFile,
    complete: boolean,
    created: number,
    availablePlugins: Array<string>
  ) {
    this.availablePlugins = availablePlugins
    this.id = id
    this.title = title
    this.description = description
    this.graphic = graphic
    this.interview = interview
    this.introduction = introduction
    this.complete = complete
    this.created = created
  }
}

export class Settings {
  private readonly client: Client

  constructor(client: Client) {
    this.client = client
  }

  async settings(): Promise<Array<SettingsPage>> {
    const q = `
            query {
                settings { 
                 valid 
                 category 
                 settings  {
                  name 
                  valid 
                  value
                 }
                }
            }
        `
    const json = await graphqlClient.query(q, {})
    return json.data['settings']
  }
}

export class ManagedFiles {
  async getManagedFileById(id: number): Promise<ManagedFile> {
    const q = `
        query ($id: ID) {
          managedFileById( id : $id )  { 
            id, bucket, folder, filename, size, written ,contentType
          }
         }
        `
    const result = await graphqlClient.query(q, { id: id })
    const managedFileId = await result.data['managedFileById']
    return managedFileId as ManagedFile
  }
}

export const ai = new Ai(graphqlClient)
export const mogul = new Mogul(graphqlClient)
export const events = mitt()
export const podcasts = new Podcasts()
export const managedFiles = new ManagedFiles()
export const settings = new Settings(graphqlClient)
