export class Podcast {

  title: string
  id: number
  uid: string
  html: string
  date: Date


  constructor(id: number, date: Date, html: string, title: string, uid: string) {
    this.title = title
    this.id = id
    this.html = html
    this.uid = uid
    this.date = date
    console.log('calling Podcast constructor')
  }
}

export class PodcastDraft {
  id: number
  completed: Boolean
  uid: string
  title: string
  description: string
  uploadPath: string
  date: Date

  constructor(
    uploadPath: string,
    id: number,
    completed: boolean,
    uid: string,
    created: Date,
    title: string,
    description: string
  ) {
    this.date = created
    this.uploadPath = uploadPath
    this.description = description
    this.title = title
    this.uid = uid
    this.completed = completed
    this.id = id
    console.log('running the Podcast constructor.')
  }
}
