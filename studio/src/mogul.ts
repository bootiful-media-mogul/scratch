import {graphqlJson} from "@/graphql";

export default class Mogul {

    constructor() {
    }

    async me() {
        return graphqlJson(`
            query { 
             me { name } 
            }
        `, new Map<String, Object>())
    }
}


