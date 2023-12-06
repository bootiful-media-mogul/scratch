import type {PodcastDraft} from '@/model'

import {cacheExchange, Client, fetchExchange} from '@urql/core'
import {resolveTransitionHooks} from "vue";

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

    async createPodcastDraft(uid: String): Promise<Awaited<unknown>[]> {
        const mutation = `
         mutation CreatePodcastDraft ( $uid: String) { 
          createPodcastDraft( uid : $uid ){ 
            
            id ,                                  
            completed ,                       
            uid   ,
            uploadPath , 
            pictureFileName , 
            introductionFileName , 
            interviewFileName  ,
            title  ,
            description 
          }
         }
        `
        const result = await this.client.mutation(mutation, {
            uid: uid
        })
        console.log(JSON.stringify(await result.data))
        return (await result.data['createPodcastDraft'] as PodcastDraft)

    }

}

export function client() {
    return new MogulClient()
}
