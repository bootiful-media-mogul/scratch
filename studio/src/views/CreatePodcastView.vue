<template>
  <h1>Create Podcast</h1>
  <form ref="createPodcastForm">
    <div>
      <h2>title</h2>
      <input type="text" v-model="title" />
    </div>
    <div>
      <h2>description</h2>
      <textarea rows="10" cols="30" v-model="description"></textarea>
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
import MogulClient from '@/mogulClient'
import axios from 'axios'

const api = new MogulClient()

function getFileFrom(event: any) {
  const fileList = event.target['files'] as FileList
  const first = fileList.item(0)
  return first as File
}

export default {
  components: {},

  data() {
    return {
      title: '',
      description: '',
      picture: null,
      intro: null,
      interview: null
    }
  },
  async created() {},

  methods: {
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

      console.log('title:' + this.title)
      console.log('description:' + this.description)

      const uid = '123'
      const result = await api.createPodcastDraft(uid)
      const uploadPath: string = '/api' + result.uploadPath

      console.log('going to upload to ' + uploadPath)
      const data = new FormData(this.$refs.createPodcastForm as HTMLFormElement)
      data.set('picture', this.picture)
      data.set('interview', this.interview)
      data.set('intro', this.intro)
      data.set('title', this.title)
      data.set('description', this.description)

      const response = await axios.post(uploadPath, data, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      console.log('upload response: ' + response)
    }

    /*
          const reader = new FileReader()
          if (this.picture != null) {
            reader.readAsDataURL(this.picture['value'])
            reader.onload = async () => {
              const encodedFile = (reader?.result as string)?.split(",")[1];
              const data = {
                file: encodedFile,
              };
              try {
                const endpoint = "/api/podcasts";
                const response = await axios.post(endpoint, data);
                console.log(response.data);
              } catch (error) {
                console.error(error);
              }
            };

          }//
          else throw new Error('you must specify a valid picture, interview, and introduction')
          */
  }

  /*

      uploadFile(event) {
        this.file.value = event.target.files[0];
      },

      async submitFile() {
        const reader = new FileReader();

        reader.readAsDataURL( this.file?.value);

        reader.onload = async () => {
          const encodedFile = reader?.result?.split(",")[1];
          const data = {
            file: encodedFile,
            fileName:  this.fileName.value,
            fileExtension: this.fileExtension.value,
            fileMimeType: this.fileMimeType.value,
          };
          try {
            const endpoint = "/api/podcasts";
            const response = await axios.post(endpoint, data);
            console.log(response.data);
          } catch (error) {
            console.error(error);
          }
        };
      } */
}
</script>
