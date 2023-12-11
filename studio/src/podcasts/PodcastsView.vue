<template>
  <h1>Podcasts</h1>


<form class="pure-form pure-form-aligned">
  <fieldset>
    <legend>
      Podcasts
    </legend>
    <div v-for="podcast in podcasts" v-bind:key="podcast.id">
      <b> {{ podcast.id }}</b> {{ podcast.title }}

      <input type="submit" @click="navigateToEpisodesPageForPodcast(podcast.id, $event)" value="new episode"/>
      <input type="submit" :disabled="podcasts.length  == 1" @click="deletePodcast(podcast.id)" value="delete"/>
    </div>

  </fieldset>
</form>


  <form class="pure-form pure-form-aligned">
    <fieldset>
      <legend>  New Podcast </legend>
      <div class="pure-control-group">
        <label for="title"> title </label>
        <input type="text" required  id="title" v-model="title"/>

        <AiWorkshopItIconComponent
            prompt="please help me take the following podcast title and make it more pithy and exciting"
            :text="title"
            @ai-workshop-completed="title = $event.text"
        />

      </div>
      <div class="pure-controls">
        <button class="pure-button pure-button-primary " type="submit"
                :disabled="title == null ||  title.trim().length  == 0"
                @click="createPodcast"
                value="create">create</button>
      </div>
    </fieldset>
  </form>
</template>
<script lang="ts">
import {Podcast, podcasts} from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'
import CreateEpisodeView from "@/podcasts/EpisodesView.vue";

async function refresh() {
  return await podcasts.podcasts()
}

export default {
  computed: {
    CreateEpisodeView() {
      return CreateEpisodeView
    }
  },
  components: {AiWorkshopItIconComponent},

  async created() {
    this.podcasts = await refresh()
  },

  methods: {
    async deletePodcast(id: number) {
      const deleted = await podcasts.delete(id)
      // nb: i tried just setting the variable podcasts to a new array, but vue.js didn't 'see' that
      // so it's safer to modify the existing collection
      this.podcasts = this.podcasts.filter((p) => p.id != deleted)
    },


    async navigateToEpisodesPageForPodcast(podcastId: number, e: Event) {
      e.preventDefault()
      console.log('creating podcast episode')
      this.$router.push({
        name: 'create-podcast-episode',
        params: {podcastId: podcastId}
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
