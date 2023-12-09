<template>
  <h1>Episodes</h1>

  <div v-if="drafts.length > 0">
    <h2>Drafts</h2>
    <div v-bind:key="podcast.id" v-for="podcast in drafts">
      <PodcastDraftComponent :podcast="podcast" />
    </div>
  </div>
  <div>
    <h2>Published</h2>
    <div v-bind:key="podcast.id" v-for="podcast in podcasts">
      <PodcastComponent @episode-deleted="deletePodcast(podcast)" :podcast="podcast" />
    </div>
  </div>
</template>

<script lang="ts">
import PodcastComponent from '@/components/PodcastComponent.vue'
import { type Podcast, PodcastDraft } from '@/model'
import { mogul } from '@/services'
import PodcastDraftComponent from '@/components/PodcastDraftComponent.vue'

export default {
  components: { PodcastDraftComponent, PodcastComponent },

  async created() {
    this.drafts = await mogul.podcastDrafts()
    this.podcasts = await mogul.podcasts()
  },
  methods: {
    async deletePodcast(podcast: Podcast) {
      console.log('deleted the podcast [' + podcast + ']')
      await mogul.deletePodcast(podcast)
    }
  },
  data() {
    return {
      podcasts: [] as Array<Podcast>,
      drafts: [] as Array<PodcastDraft>
    }
  }
}
</script>
