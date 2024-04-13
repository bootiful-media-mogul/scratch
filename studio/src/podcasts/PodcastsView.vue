<template>
  <h1>{{ $t('podcasts.title') }}</h1>
  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>{{ $t('podcasts.new-podcast') }}</legend>
      <div class="pure-control-group">
        <label for="title">
          {{ $t('podcasts.new-podcast.title') }}

          <AiWorkshopItIconComponent
            :prompt="$t('podcasts.title.ai.prompt')"
            :text="title"
            @ai-workshop-completed="title = $event.text"
          />
        </label>
        <input type="text" required id="title" v-model="title" />
      </div>
      <div class="pure-controls">
        <button
          class="pure-button pure-button-primary"
          type="submit"
          :disabled="title == null || title.trim().length == 0"
          @click="createPodcast"
          value="create"
        >
          {{ $t('podcasts.new-podcast.submit') }}
        </button>
      </div>
    </fieldset>
  </form>
  <form class="pure-form">
    <fieldset>
      <legend>Podcasts</legend>
      <div class="pure-g form-row podcast-rows" v-for="podcast in podcasts" v-bind:key="podcast.id">
        <div class="id-column">
          #<b>{{ podcast.id }}</b>
        </div>

        <div class="links-column">
          <a href="#" @click="navigateToEpisodesPageForPodcast(podcast.id, $event)">
            {{ $t('podcasts.podcasts.episodes') }}
          </a>
          |
          <a v-if="podcasts.length > 1" href="#" @click="deletePodcast(podcast.id)">
            {{ $t('podcasts.podcasts.delete') }}
          </a>
          <a v-if="podcasts.length == 1" href="#" class="disabled">
            {{ $t('podcasts.podcasts.delete') }}
          </a>
        </div>
        <div class="title-column">
          {{ podcast.title }}
        </div>
      </div>
    </fieldset>
  </form>
</template>

<style>
.id-column {
  font-weight: normal;
  font-size: smaller;
}

.links-column {
}

.title-column {
}

.id-column b {
  font-weight: bold;
  font-size: medium;
}

.podcast-rows {
  display: grid;
  grid-template-areas: 'id   links  title ';
  grid-template-columns: 50px 200px auto;
}
</style>
<script lang="ts">
import { Podcast, podcasts } from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'
import CreateEpisodeView from '@/podcasts/EpisodesView.vue'

async function refresh() {
  return await podcasts.podcasts()
}

export default {
  computed: {
    CreateEpisodeView() {
      return CreateEpisodeView
    }
  },
  components: { AiWorkshopItIconComponent },

  async created() {
    this.podcasts = await refresh()
  },

  methods: {
    async deletePodcast(id: number) {
      console.log('trying to delete ' + id)
      const deleted = await podcasts.deletePodcast(id)
      // nb: i tried just setting the variable podcasts to a new array, but vue.js didn't 'see' that
      // so it's safer to modify the existing collection
      this.podcasts = this.podcasts.filter((p) => p.id != deleted)
    },

    async navigateToEpisodesPageForPodcast(podcastId: number, e: Event) {
      e.preventDefault()
      console.log('creating podcast episode')
      this.$router.push({
        name: 'podcast-episodes',
        params: { id: podcastId }
      })
    },
    async createPodcast(e: Event) {
      e.preventDefault()
      await podcasts.create(this.title)
      this.podcasts = await refresh()
      this.title = ''
    }
  },

  data() {
    return {
      podcasts: [] as Array<Podcast>,
      title: ''
    }
  }
}
</script>
