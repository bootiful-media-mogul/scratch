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
      this.currentPodcast = await podcasts.podcastById(newPodcastId)
      this.episodes = await podcasts.podcastEpisodes(newPodcastId)
    },

    async loadEpisode(episode: Episode) {

      this.draftEpisode.id = episode.id
      this.draftEpisode.interview = episode.interview
      this.draftEpisode.introduction = episode.introduction
      this.draftEpisode.graphic = episode.graphic
      this.draftEpisode.title = episode.title
      this.draftEpisode.description = episode.description

      this.description = this.draftEpisode.description
      this.title = this.draftEpisode.title
      this.dirtyKey = this.computeDirtyKey()
      await this.loadPodcast()
    },

    async save(e: Event) {
      e.preventDefault()

      if (this.draftEpisode.id) {
        // we're editing a record, so update it
        const episode = await podcasts.updatePodcastEpisode(
            this.draftEpisode.id, this.title, this.description
        )
        await this.loadEpisode(episode)
      }//
      else {
        const episode = await podcasts.createPodcastEpisodeDraft(
            this.selectedPodcastId,
            this.title,
            this.description
        )
        await this.loadEpisode(episode)
      }

    },

    changed(): boolean {
      function isEmpty(txt: string): boolean {
        return txt == null || txt.trim().length == 0
      }

      const empty = (isEmpty(this.title) || isEmpty(this.description)) as boolean
      const dirty = this.computeDirtyKey() != this.dirtyKey

      return (!empty && dirty) as boolean
    },

    computeDirtyKey() {
      return '' + this.description + ':' + this.title

    }

  },

  data() {
    return {
      draftEpisode: reactive({} as Episode),
      episodes: [] as Array<Episode>,
      podcasts: [] as Array<Podcast>,
      currentPodcast: null as any as Podcast,
      selectedPodcastId: this.id,
      title: '',
      description: '',
      dirtyKey: ''
    }
  }
}
</script>
<style>

  .episode-managed-file-row {
    height: calc(var(--gutter-space) * 1);
    margin-bottom : var(--gutter-space);
    margin-top:  var(--gutter-space);
  }
  .episode-managed-file-row label {
    padding:  0; margin:  0;
  }
</style>
<template>
  <h1 v-if="currentPodcast">Episodes for "{{ currentPodcast.title }}"</h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>
        <span v-if=" title">Editing "{{ title }}"</span>
        <span v-else>
           New   Episode
        </span>
      </legend>

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

      <div v-if="draftEpisode" >
        <div v-if="draftEpisode.graphic" class="pure-g  episode-managed-file-row ">
          <div class="pure-u-4-24"><label>graphic</label></div>
          <div class="pure-u-20-24">
            <ManagedFileComponent
                accept=".jpg,.jpeg,.png,image/jpeg,image/jpg,image/png"
                v-model:managed-file-id="draftEpisode.graphic.id"/>
          </div>
        </div>
        <div v-if="draftEpisode.introduction" class="pure-g  episode-managed-file-row">
          <div class="pure-u-4-24"><label>introduction</label></div>
          <div class="pure-u-20-24">
            <ManagedFileComponent
                accept=".mp3,audio/mpeg"
                v-model:managed-file-id="draftEpisode.introduction.id"/>
          </div>
        </div>
        <div v-if="draftEpisode.interview" class="pure-g  episode-managed-file-row" >
          <div class="pure-u-4-24"><label>interview</label></div>
          <div class="pure-u-20-24">
            <ManagedFileComponent v-model:managed-file-id="draftEpisode.interview.id"
                                  accept=".mp3,audio/mpeg"
            />
          </div>
        </div>
      </div>

      <button
          @click="save"
          :disabled="!changed()"
          type="submit"
          class="pure-button pure-button-primary"
      >
        save
      </button>


    </fieldset>
  </form>

  <form class="pure-form">
    <fieldset>
      <legend>Episodes</legend>

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
