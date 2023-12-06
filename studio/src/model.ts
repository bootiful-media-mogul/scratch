export class Podcast {
    title: string
    id: number

    constructor(id: number, title: string) {
        this.title = title
        this.id = id
    }
}

export class PodcastDraft {
    id: number
    completed: Boolean
    uid: string
    date: string
    title: string
    description: string
    uploadPath: string

    constructor(
        up: string,
        id: number,
        completed: boolean,
        uid:   string,
        date:  string,
        title: string,
        description: string
    ) {
        this.uploadPath = up
        this.description = description
        this.title = title
        this.uid = uid
        this.date = date
        this.completed = completed
        this.id = id
    }
}
