import { Client } from '@urql/core'
// import { Podcast, PodcastDraft } from '@/model'

export default class Mogul {
  private readonly client: Client

  constructor(client: Client) {
    this.client = client
  }
  /*

  async podcastDrafts() {
    const query = `
                query {
                  podcastDrafts { 
                    id  
                    uid  
                    created  
                    title  
                    description  
                    uploadPath 
                  } 
                }
        `
    type GraphqlPodcast = {
      id: number
      created: number
      title: string
      uid: string
      completed: boolean
      uploadPath: string
      description: string
    }
    const queryResult = await this.client.query(query, {})
    const podcasts = this.indexIntoQueryField(queryResult, 'podcastDrafts') as Array<GraphqlPodcast>
    return podcasts.map(
      (podcast: GraphqlPodcast) =>
        new PodcastDraft(
          podcast.uploadPath,
          podcast.id,
          podcast.completed,
          podcast.uid,
          new Date(podcast.created),
          podcast.title,
          podcast.description
        )
    )
  }

  async podcasts() {
    const query = `
            query { 
              podcasts { 
                created  
                html  
                title  
                description 
                uid 
                id 
              }
            } 
    `
    type GraphqlPodcast = { id: number; created: number; html: string; title: string; uid: string }
    const queryResult = await this.client.query(query, {})
    const podcasts = this.indexIntoQueryField(queryResult, 'podcasts') as Array<GraphqlPodcast>
    return podcasts.map(
      (podcast: GraphqlPodcast) =>
        new Podcast(podcast.id, new Date(podcast.created), podcast.html, podcast.title, podcast.uid)
    )
  }
*/

  async me(): Promise<string> {
    const query = `
            query { 
             me { name } 
            } 
    `
    const result = await this.client.query(query, {})
    return this.indexIntoQueryField(result, 'me')['name'] as string
  }

  private indexIntoQueryField(result: any, resultKey: string): any {
    return result.data[resultKey]
  }
  /*
  async createPodcastDraft(uid: String): Promise<PodcastDraft> {
    const mutation = `
         mutation CreatePodcastDraft ( $uid: String) { 
          createPodcastDraft( uid : $uid ){ 
            id,                                  
            completed,                       
            uid,
            uploadPath, 
            pictureFileName, 
            introductionFileName, 
            interviewFileName,
            title,
            description 
          }
         }
        `
    const result = await this.client.mutation(mutation, {
      uid: uid
    })
    return (await result.data['createPodcastDraft']) as PodcastDraft
  }

  async deletePodcast(podcast: Podcast) {
    const mutation = `
         mutation DeletePodcast  ( $id: Int) { 
          deletePodcast(  id : $id ) 
         }
        `
    const result = await this.client.mutation(mutation, {
      id: podcast.id
    })

    return (await result.data['deletePodcast']) as Boolean
  }*/
}
