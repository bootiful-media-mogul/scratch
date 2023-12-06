import type {PodcastDraft} from '@/model'

import {cacheExchange, Client, fetchExchange} from '@urql/core'

export default class MogulClient {

    client: Client

    constructor() {
        this.client = new Client({
            url: '/api/graphql',
            exchanges: [cacheExchange, fetchExchange]
        })
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
           id, uid, complete, uploadPath
          }
         }
        `
        const result = await this.client.mutation(mutation, {
            'uid': uid
        })
        return result.data ['createPodcastDraft'] as PodcastDraft
    }

    /*    async getPodcasts(): Array<Podcast> {
            const results = graphqlJson(
                `
             query {
              podcasts { id, title, description  }
             }
            `,
                {} as Map<String, Object>
            )

            console.log(results)
            return results
        }*/
}

export function client() {
    return new MogulClient()
}