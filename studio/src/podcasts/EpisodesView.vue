<script lang="ts">
import { ref } from 'vue'
import { ManagedFile, Episode, Podcast, podcasts } from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'

export default {
  components: { AiWorkshopItIconComponent /*, ManagedFileComponent*/ },

  props: ['podcastId'],

  methods: {
    async createDraft() {
      if (this.isValidPodcastDraft()) {
        const episode = await podcasts.createPodcastEpisodeDraft(
          this.podcast.id,
          this.title,
          this.description
        )
        console.log(JSON.stringify(episode))
      }
    },
    isValidPodcastDraft(): boolean {
      function isEmpty(txt: string): boolean {
        return txt == null || txt.trim().length == 0
      }

      const empty = (isEmpty(this.title) || isEmpty(this.description)) as boolean
      return !empty
    }
  },
  async beforeMount() {
    const possiblePodcasts = await podcasts.podcasts()
    for (let i = 0; i < possiblePodcasts.length; i++) this.podcasts.push(possiblePodcasts[i])
    this.podcast = this.podcasts.filter((p) => p.id == this.podcastId)[0]
    this.episodes = await podcasts.podcastEpisodes(this.podcastId)
    this.$forceUpdate()
  },
  setup() {
    return {
      episodes: [] as Array<Episode>,
      managedFile: null as any as ManagedFile,
      podcast: null as any as Podcast,
      podcasts: [] as Array<Podcast>,
      title: ref(''),
      description: ref(''),
      intro: ref(null as any),
      interview: ref(null as any),
      photo: ref(null as any)
    }
  }
}
</script>
<template>
  <h1 v-if="podcast">Episodes for {{ podcast.title }}</h1>
  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>Create a New Podcast Episode</legend>

      <div class="pure-control-group">
        <label for="podcastSelection">podcast</label>

        <select id="podcastSelection" v-model="podcast">
          <option v-for="option in podcasts" :value="option" :key="option.id">
            {{ option.id }} - {{ option.title }}
          </option>
        </select>
      </div>
      <div class="pure-control-group">
        <label>title</label>
        <input required v-model="title" type="text" />
        <AiWorkshopItIconComponent
          prompt="please help me make the following podcast title more pithy and exciting"
          :text="title"
          @ai-workshop-completed="title = $event.text"
        />
      </div>
      <div class="pure-control-group">
        <label>description</label>
        <textarea rows="10" required v-model="description" />
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
      <!--      <div class="pure-control-group">
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
            </div>-->
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

  <!--

    <form class="pure-form">
      <fieldset>
        <legend>Podcasts</legend>
        <div class="pure-g podcast-row" v-for="podcast in podcasts" v-bind:key="podcast.id">
          <div class="pure-u-1-24">
            <b> {{ podcast.id }}</b>
          </div>

          <div class="pure-u-6-24">
            <a href="#" @click="navigateToEpisodesPageForPodcast(podcast.id, $event)"> episodes</a>

            |
            <a v-if="podcasts.length > 1" href="#" @click="deletePodcast(podcast.id)"> delete </a>
            <a v-if="podcasts.length == 1" href="#" class="disabled"> delete </a>
          </div>

          <div class="pure-u-17-24">
            {{ podcast.title }}
          </div>
        </div>
      </fieldset>
    </form>


  -->
</template>
