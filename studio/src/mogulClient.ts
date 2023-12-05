import {graphqlJson} from "@/graphql";

export default class MogulClient {

    constructor() {
    }

    async me() {
        return graphqlJson(`
            query { 
             me { name } 
            }
        `, new Map<String, Object>());
    }

    async podcasts() {
        const results =   graphqlJson(`
         query { 
          podcasts { id, title, description  }
         }
        `, new Map<String, Object>());

        console.log(results)
        return results
    }
}


