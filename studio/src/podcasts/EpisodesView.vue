<script lang="ts">
import { Episode, Podcast, podcasts } from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'
import ManagedFileComponent from '@/managedfiles/ManagedFileComponent.vue'
import { reactive } from 'vue'

export default {
  mounted(): void {
    console.log('mounted()')
    this.refreshRecords()
  },

  components: {
    ManagedFileComponent,
    AiWorkshopItIconComponent
  },

  props: ['id'],

  methods: {
    async editEpisode(episode: Episode) {
      console.log('you want to edit ' + JSON.stringify(episode))

      this.draftEpisode.id = episode.id
      this.draftEpisode.interview = episode.interview
      this.draftEpisode.introduction = episode.introduction
      this.draftEpisode.graphic = episode.graphic
      this.draftEpisode.title = episode.title
      this.draftEpisode.descriptions = episode.description
    },
    async createDraft() {
      if (this.isEpisodeReadyForFiles()) {
        const episode = await podcasts.createPodcastEpisodeDraft(
          this.selectedPodcastId,
          this.title,
          this.description
        )

        await this.editEpisode(episode)
      }
    },

    isEpisodeReadyForFiles(): boolean {
      function isEmpty(txt: string): boolean {
        return txt == null || txt.trim().length == 0
      }

      const empty = (isEmpty(this.title) || isEmpty(this.description)) as boolean
      return !empty
    },

    async refreshRecords() {
      const newPodcastId = this.selectedPodcastId
      console.log('podcastId: ' + newPodcastId)
      this.podcasts = await podcasts.podcasts()

      this.currentPodcast = this.podcasts.filter((p) => p.id == newPodcastId)[0]
      this.episodes = await podcasts.podcastEpisodes(newPodcastId)
    }
  },

  data() {
    return {
      draftEpisode: reactive({}),
      episodes: [] as Array<Episode>,
      podcasts: [] as Array<Podcast>,
      currentPodcast: null as any as Podcast,
      selectedPodcastId: this.id,
      title: '',
      description: ''
      // graphic: reactive({}),
      // introduction: reactive({}),
      // interview: reactive({}),
    }
  }
}
</script>
<template>
  <h1 v-if="currentPodcast">Episodes for "{{ currentPodcast.title }}"</h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>Create a New Podcast Episode</legend>

      <label for="podcastSelect">podcast</label>
      <select id="podcastSelect" v-model="selectedPodcastId" @change="refreshRecords">
        <option v-for="podcast in podcasts" :key="podcast.id" :value="podcast.id">
          {{ podcast.id }} - {{ podcast.title }}
        </option>
      </select>

      <label for="episodeTitle">
        title
        <AiWorkshopItIconComponent
          prompt="please help me make the following podcast title more pithy and exciting"
          :text="title"
          @ai-workshop-completed="title = $event.text"
        />
      </label>
      <input id="episodeTitle" required v-model="title" type="text" />

      <label for="episodeDescription">
        description
        <AiWorkshopItIconComponent
          prompt="please help me make the following podcast description more pithy and exciting"
          :text="description"
          @ai-workshop-completed="description = $event.text"
        />
      </label>
      <textarea id="episodeDescription" rows="10" required v-model="description" />

      <div v-if="draftEpisode">
        <div v-if="draftEpisode.graphic">
          <label>photo</label>
          <ManagedFileComponent v-model:managed-file-id="draftEpisode.graphic.id" /> {{draftEpisode.graphic.id}}
        </div>
        <div v-if="draftEpisode.introduction">
          <label>introduction</label>
          <ManagedFileComponent v-model:managed-file-id="draftEpisode.introduction.id" />
          {{draftEpisode.introduction.id}}
        </div>
        <div v-if="draftEpisode.interview">
          <label>interview</label>
          <ManagedFileComponent v-model:managed-file-id="draftEpisode.interview.id" />
          {{draftEpisode.interview.id}}
        </div>
      </div>

      <button
        @click="createDraft"
        :disabled="!isEpisodeReadyForFiles()"
        type="submit"
        class="pure-button pure-button-primary"
      >
        save
      </button>
    </fieldset>
  </form>

  <form class="pure-form">
    <fieldset>
      <legend>Past Episodes</legend>

      <div class="pure-g form-row" v-bind:key="episode.id" v-for="episode in episodes">
        <div class="pure-u-1-24">
          <b> {{ episode.id }}</b>
        </div>

        <div class="pure-u-3-24">
          <a href="#" @click="editEpisode(episode)">edit</a>
        </div>
        <div class="pure-u-20-24">{{ episode.title }}</div>
      </div>
    </fieldset>
  </form>
  <!--
   -->
</template>
