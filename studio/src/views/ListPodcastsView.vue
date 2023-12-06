<template>
  <div>
    <h1>List Podcasts</h1>
  </div>
  <div v-bind:key="podcast.id" v-for="podcast in podcasts">
    <PodcastComponent :podcast="podcast" />
  </div>
</template>

<script lang="ts">
import MogulClient from '@/mogulClient'
import PodcastComponent from '@/components/PodcastComponent.vue'
import type { Podcast } from '@/model'

const api = new MogulClient()

export default {
  components: { PodcastComponent },
  data() {
    return {
      podcasts: [] as Array<Podcast>
    }
  },
  async created() {
    // this.podcasts = await api.podcasts()
    // console.log('podcasts: ' + this.podcasts)
  }
}
</script>
