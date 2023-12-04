export function graphql(graphqlQuery: String, variables: Map<String, Object>) {
    const url = '/api/graphql'
    return window.fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        },
        body: JSON.stringify({
            query: graphqlQuery,
            variables: variables,
        })
    })

}

export async function graphqlJson(query: String, variables: Map<String, Object>) {
    return (await (await graphql(query, variables)).json())
}