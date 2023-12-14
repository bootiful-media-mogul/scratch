<script lang="ts">
import {Episode, Podcast, podcasts} from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'
import ManagedFileComponent from '@/managedfiles/ManagedFileComponent.vue'
import {reactive} from 'vue'

export default {
  mounted(): void {
    this.loadPodcast()
  },

  components: {
    ManagedFileComponent,
    AiWorkshopItIconComponent
  },

  props: ['id'],

  methods: {

    async loadPodcast() {
      const newPodcastId = this.selectedPodcastId
      // this.podcasts = await podcasts.podcasts()
      this.currentPodcast = await podcasts.podcastById(newPodcastId)
      this.episodes = await podcasts.podcastEpisodes(newPodcastId)
    },

    async loadEpisode(episode: Episode) {
      console.log('you want to edit ' + JSON.stringify(episode))

      this.draftEpisode.id = episode.id
      this.draftEpisode.interview = episode.interview
      this.draftEpisode.introduction = episode.introduction
      this.draftEpisode.graphic = episode.graphic
      this.draftEpisode.title = episode.title
      this.draftEpisode.description = episode.description

      this.description = this.draftEpisode.description
      this.title = this.draftEpisode.title
      await this.loadPodcast()
    },
    async cancelChanges(e: Event) {
      e.preventDefault()

    },
    async createDraft(e: Event) {
      e.preventDefault()
      if (this.isEpisodeReadyForFiles()) {
        const episode = await podcasts.createPodcastEpisodeDraft(
            this.selectedPodcastId,
            this.title,
            this.description
        )
        await this.loadEpisode(episode)
      }
    },

    isEpisodeReadyForFiles(): boolean {
      function isEmpty(txt: string): boolean {
        return txt == null || txt.trim().length == 0
      }

      const empty = (isEmpty(this.title) || isEmpty(this.description)) as boolean
      return !empty
    },


  },

  data() {
    return {
      draftEpisode: reactive({} as Episode),
      episodes: [] as Array<Episode>,
      podcasts: [] as Array<Podcast>,
      currentPodcast: null as any as Podcast,
      selectedPodcastId: this.id,
      title: '',
      description: ''
    }
  }
}
</script>
<template>
  <h1 v-if="currentPodcast">Episodes for "{{ currentPodcast.title }}"</h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>
        <span v-if="draftEpisode.title">Editing "{{ draftEpisode.title }}"</span>
        <span v-else>
          Create a New Podcast Episode
        </span>
      </legend>

      <!--
            <label for="podcastSelect">podcast</label>
            <select  id="podcastSelect" v-model="selectedPodcastId" @change="refreshRecords">
              <option v-for="podcast in podcasts" :key="podcast.id" :value="podcast.id">
                {{ podcast.id }} - {{ podcast.title }}
              </option>
            </select>
      -->

      <label for="episodeTitle">
        title
        <AiWorkshopItIconComponent
            prompt="please help me make the following podcast title more pithy and exciting"
            :text="title"
            @ai-workshop-completed="title = $event.text"
        />
      </label>
      <input id="episodeTitle" required v-model="title" type="text"/>

      <label for="episodeDescription">
        description
        <AiWorkshopItIconComponent
            prompt="please help me make the following podcast description more pithy and exciting"
            :text="description"
            @ai-workshop-completed="description = $event.text"
        />
      </label>
      <textarea id="episodeDescription" rows="10" required v-model="description"/>

      <div v-if="draftEpisode">
        <div v-if="draftEpisode.graphic">
          <label>photo</label>
          <ManagedFileComponent v-model:managed-file-id="draftEpisode.graphic.id"/>


        </div>
        <div v-if="draftEpisode.introduction">
          <label>introduction</label>
          <ManagedFileComponent v-model:managed-file-id="draftEpisode.introduction.id"/>
        </div>
        <div v-if="draftEpisode.interview">
          <label>interview</label>
          <ManagedFileComponent v-model:managed-file-id="draftEpisode.interview.id"/>
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
      <!--
      <button
          @click="cancelChanges"
          :disabled="!draftEpisode.id "
          type="submit"
          class="pure-button"
      >
        cancel
      </button>
      -->
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
          <a href="#" @click="loadEpisode(episode)">edit</a>
        </div>
        <div class="pure-u-20-24">{{ episode.title }}</div>
      </div>
    </fieldset>
  </form>
  <!--
   -->
</template>
