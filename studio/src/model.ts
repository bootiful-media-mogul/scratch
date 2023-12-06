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
  complete: Boolean
  uid: String
  date: String
  title: String
  description: String

  constructor(
    id: number,
    complete: boolean,
    uid: String,
    date: String,
    title: String,
    description: String
  ) {
    this.description = description
    this.title = title
    this.uid = uid
    this.date = date
    this.complete = complete
    this.id = id
  }
}
