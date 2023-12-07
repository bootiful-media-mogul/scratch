import type { PodcastDraft } from '@/model'

import { Client } from '@urql/core'

export default class MogulClient {
  private readonly client: Client

  constructor(client: Client) {
    this.client = client
    console.debug('initializing MogulClient')
  }

  async me(): Promise<string> {
    const query = `
            query { 
             me { name } 
            } 
    `
    const result = await this.client.query(query, {})
    return result.data['me']['name']
  }

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
}
