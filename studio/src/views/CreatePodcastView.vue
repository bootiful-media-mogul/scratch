<template>
  <h1>Create Podcast</h1>
  <div>
    <h2>title</h2>
    <input type="text" :value="title" />
  </div>
  <div>
    <h2>description</h2>
    <textarea rows="10" cols="30" :value="description" />
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

  <button @click="submit">Submit</button>
</template>

<script lang="ts">
import MogulClient from '@/mogulClient'
import axios from 'axios'

const api = new MogulClient()

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
      this.picture = event.target['files'][0]
    },
    uploadIntro(event: any) {
      this.intro = event.target['files'][0]
    },
    uploadInterview(event: any) {
      this.interview = event.target['files'][0]
    },

    async submit() {
      const uid = '123'
      const result = await api.createPodcastDraft(uid)
      console.log('re: ' + JSON.stringify(result))

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
}
</script>
