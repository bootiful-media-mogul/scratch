<template>
  <h1>Create Podcast</h1>
  <form ref="createPodcastForm">
    <div>
      <h2>title</h2>
      <input type="text" v-model="title" />
      <br />
      <AiWorkshopItIcon
        @ai-workshop-completed="afterTitleWorkshop"
        prompt="Make the following podcast title more lively:"
        :text="title"
      />
    </div>
    <div>
      <h2>description</h2>
      <textarea rows="10" cols="30" v-model="description"></textarea>
      <br />
      <AiWorkshopItIcon
        @ai-workshop-completed="afterDescriptionWorkshop"
        prompt="Make the following podcast description more lively:"
        :text="description"
      />
    </div>
    <div>
      <h2>picture</h2>
      <input type="file" @change="uploadPicture" />
    </div>

    <div>
      <h2>intro</h2>
      <input type="file" @change="uploadIntro" />
    </div>

    <div>
      <h2>interview</h2>
      <input type="file" @change="uploadInterview" />
    </div>

    <div>
      <button @click="submit">Submit</button>
    </div>
  </form>
</template>

<script lang="ts">
import {  mogul, AiWorkshopReplyEvent } from '@/services'
import axios from 'axios'
import AiWorkshopItIcon from '@/components/AiWorkshopItIconComponent.vue'

function getFileFrom(event: any) : File {
  const fileList = event.target['files'] as FileList
  const first = fileList.item(0)
  return first as File
}

export default {
  components: { AiWorkshopItIcon },

  data() {
    return {
      title: '',
      description: '',
      picture: null as null | File,
      intro: null as null | File,
      interview: null as null | File
    }
  },
  async created() {
  },


  methods: {
    afterDescriptionWorkshop(updated: AiWorkshopReplyEvent) {
      this.description = updated.text
    },

    afterTitleWorkshop(updated: AiWorkshopReplyEvent) {
      this.title = updated.text
    },

    uploadPicture(event: any) {
      this.picture = getFileFrom(event)
    },
    uploadIntro(event: any) {
      this.intro = getFileFrom(event)
    },
    uploadInterview(event: any) {
      this.interview = getFileFrom(event)
    },

    async submit(event: Event) {
      event.preventDefault()

      const uid = crypto.randomUUID()
      const result = await mogul.createPodcastDraft(uid)
      const uploadPath: string = '/api' + result.uploadPath

      const data = new FormData(this.$refs.createPodcastForm as HTMLFormElement)
      data.set('picture', this.picture as File)
      data.set('interview', this.interview as File)
      data.set('intro', this.intro as File)
      data.set('title', this.title)
      data.set('description', this.description)

      const response = await axios.post(uploadPath, data, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      console.assert(
        response.status >= 200 && response.status <= 300,
        'the http post to upload the archive did not succeed.'
      )
      console.log('uploaded to ' + uploadPath + ' vis ' + uploadPath)
    }
  }
}
</script>
