<template>
  <h1> Podcasts </h1>

  <h2>
    Existing Podcasts
  </h2>

  <div v-for="podcast in podcasts" v-bind:key="podcast.id">

    <b> {{ podcast.id }}</b> {{ podcast.title }}

    <a href="#" v-if="podcasts.length > 1" @click="deletePodcast (podcast.id)">delete</a>
    <span style="font-size: smaller" v-else>(you must have at least one podcast specified)</span>

  </div>

  <h2> New Podcast </h2>
  <form>
    <input type="text" v-model="title"/> <br/>
    <AiWorkshopItIconComponent prompt="please help me take the following podcast title and make it more pithy and exciting"
                               :text="title" @ai-workshop-completed="title = $event.text"/>
    <br/>
    <input type="submit" @click="createPodcast" value="create"/>
  </form>
</template>
<script lang="ts">
import {Podcast, podcasts} from '@/services'
import AiWorkshopItIconComponent from "@/ai/AiWorkshopItIconComponent.vue";


async function refresh() {
  return await podcasts.podcasts()
}

export default {
  components: {AiWorkshopItIconComponent},

  async created() {
    this.podcasts = await refresh()
  },

  methods: {
    async deletePodcast(id: number) {
      const deleted = await podcasts.delete(id)
      // nb: i tried just setting the variable podcasts to a new array, but vue.js didn't 'see' that
      // so it's safer to modify the existing collection
      this.podcasts = this.podcasts.filter(p => p.id != deleted)
    },

    async createPodcast(e: Event) {
      e.preventDefault()
      await podcasts.create(this.title)
      this.podcasts = await refresh()
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
