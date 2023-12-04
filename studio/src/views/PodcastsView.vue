<template>


  <div>

    <h1> Podcasts </h1>


  </div>


</template>

<script lang="ts">

export default {
  data() {
    return {me: ''}
  },
  async created() {

  },

  // ...other options
}

/*
export default {
  name: 'PodcastsView',
  mounted() {
    console.log('podcastsview')
  },
  created() {
    console.debug('starting ' + this.$options.name)
  },
  methods: {
    episodeHasBeenProducedCallback() {
      /!*   this.cancelForm()
         this.$router.push('/search')*!/
    },
    cancelForm() {
      /!*   this.title = null
         this.description = null
         this.files.photo = null
         this.files.interview = null
         this.backgroundImageUrl = null
         this.files.introduction = null
         this.published = false
         this.publicationStatusMessage = messages ['uploading']
   *!/
    },
    /!* async createEpisode() {
       if (this.formIsValid === true) {
         await this.$root.$data.createEpisode(this.title, this.description, this.files.introduction,
             this.files.interview, this.files.photo, async (productionStatus) => {
               console.log('production status:', productionStatus)
               console.log('attempting to resolve status ', productionStatus.status, 'to a local message', messages [productionStatus.status])
               this.publicationStatusMessage = messages [productionStatus.status]
               this.published = productionStatus.finished
               this.$refs['form-modal'].show()
             })
       }

     },*!/
    /!* async previewProfilePhoto() {
      /!* if (this.files.photo == null) {
         return
       }
       const theFile = await readFileReaderData(this.files.photo, (fr) => {
         return fr.readAsDataURL
       });
       if (theFile !== null) {
         this.backgroundImageUrl = `background-image: url("${theFile}")`
       }*!/
     },
    /!* isEmptyFile(f) {
       return f == null
     },
     isEmptyString(s) {
       return s == null || s.trim() === ''
     },
     isMinimumLengthString(s) {
       return s.length >= 5
     }*!/
   },*!/
    computed: {
      /!*    formIsValid: function () {
            const files = [this.files.introduction, this.files.interview, this.files.photo]
            const text = [this.title, this.description]
            const valid = files.filter(f => this.isEmptyFile(f)).length === 0 && text.filter(t => this.isEmptyString(t)).length === 0
            console.log('valid', valid)
            return valid
          }*!/
    },
    data() {
      return {
        /!*      publicationStatusMessage: '',
              published: false,
              originalBackgroundImageUrl: null,
              files: {
                interview: null,
                introduction: null,
                photo: null
              },
              backgroundImageUrl: '',
              title: '',
              description: ''*!/
      }
    },
    components: {
      // Tip,
      // Panel,
      // Page
    }
  }
}*/
</script>