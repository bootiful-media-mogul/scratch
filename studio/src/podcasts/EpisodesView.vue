<script lang="ts">
import {ref} from 'vue'
import {Episode, ManagedFile, Podcast, podcasts} from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'

export default {

  mounted(): void {
    console.log('mounted()')
    this.refreshRecords()
  },


  components: {
    AiWorkshopItIconComponent
  },

  props: ['id'],

  methods: {


    async createDraft() {
      if (this.isValidPodcastDraft()) {
        const episode = await podcasts.createPodcastEpisodeDraft(
            this.selectedPodcastId,
            this.title,
            this.description
        )
        console.log('creating a draft ' + JSON.stringify(episode))
        this.draftEpisode = episode
      }
    },


    isValidPodcastDraft(): boolean {
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
      this.currentPodcast = this.podcasts.filter(p => p.id == newPodcastId)[0]
      this.episodes = await podcasts.podcastEpisodes(newPodcastId)
    },

  },

  data(vm) {
    return {
      draftEpisode: (null as any) as Episode,
      episodes: [] as Array<Episode>,
      podcasts: [] as Array<Podcast>,
      currentPodcast: (null as any) as Podcast,
      selectedPodcastId: this.id,
      title: '',
      description: ''
    }
  },
}
</script>
<template>


  <h1 v-if="currentPodcast">Episodes for "{{ currentPodcast.title }}"</h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>Create a New Podcast Episode</legend>

      <div class="pure-control-group">
        <label for="podcastSelect">podcast</label>
        <select id="podcastSelect" v-model="selectedPodcastId" @change="refreshRecords">
          <option v-for="podcast in podcasts" :key="podcast.id" :value="podcast.id">
            {{ podcast.id }} - {{ podcast.title }}
          </option>
        </select>
      </div>

      <div class="pure-control-group">
        <label>title</label>
        <input required v-model="title" type="text"/>
        <AiWorkshopItIconComponent
            prompt="please help me make the following podcast title more pithy and exciting"
            :text="title"
            @ai-workshop-completed="title = $event.text"
        />
      </div>

      <div class="pure-control-group">
        <label>description</label>
        <textarea rows="10" required v-model="description"/>
        <AiWorkshopItIconComponent
            prompt="please help me make the following podcast description more pithy and exciting"
            :text="description"
            @ai-workshop-completed="description = $event.text"
        />
      </div>

      <div class="pure-controls">
        <button
            @click="createDraft"
            :disabled="!isValidPodcastDraft()"
            type="button"
            class="pure-button pure-button-primary"
        >
          continue
        </button>
      </div>

    </fieldset>
  </form>

  <form class="pure-form">
    <fieldset>
      <legend>Past Episodes</legend>

      <div class="pure-g form-row" v-bind:key="episode.id" v-for="episode in episodes">
        <div class="pure-u-4-24">{{ episode.id }}</div>
        <div class="pure-u-6-24">{{ episode.title }}</div>
        <div class="pure-u-14-24">{{ episode.description }}</div>
      </div>
    </fieldset>
  </form>
  <!--  <h1 v-if="podcast">Episodes for "{{ podcast.title }}"</h1>
    <form class="pure-form pure-form-stacked">
      <fieldset>
        <legend>Create a New Podcast Episode</legend>



        &lt;!&ndash;      <div class="pure-control-group">
                <label>photo</label>
                <ManagedFileComponent v-model:managed-file-id="photo" />
              </div>
              <div class="pure-control-group">
                <label>introduction</label>
                <ManagedFileComponent v-model:managed-file-id="intro" />
              </div>
              <div class="pure-control-group">
                <label>interview</label>
                <ManagedFileComponent v-model:managed-file-id="interview" />
              </div>&ndash;&gt;
      </fieldset>
    </form>
   -->


</template>
