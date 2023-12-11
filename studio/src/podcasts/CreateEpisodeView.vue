<script lang="ts">
import {defineComponent, ref} from 'vue'
import ManagedFileComponent from '@/managedfiles/ManagedFileComponent.vue'
import {Podcast, podcasts} from "@/services";

export default defineComponent({
  components: {ManagedFileComponent},
  props: ['podcastId'],

  async beforeMount() {


    const possiblePodcasts = await podcasts.podcasts()
    for (let i = 0; i < possiblePodcasts.length; i++)
      this.podcasts.push(possiblePodcasts [i])

    this.podcast   = this.podcasts.filter(p => p.id == this.podcastId) [0]
    console.log('working with ' + JSON.stringify(this.podcast))

    this.$forceUpdate()

  },
  setup( ) {
    return {
      podcast:   (null as any) as Podcast ,
      podcasts:  [] as Array<Podcast> ,
      title: '',
      description: '',
      intro: ref(null as any),
      interview: ref(null as any),
      photo: ref(null as any),
    }
  }
})
</script>
<template>

  <h2>
    Podcast Episodes
  </h2>
  <div>
    <label>podcast</label>

    <select v-model="podcast">
      <option v-for="option in podcasts" :value="option" :key="option.id">
        {{ option.id }}  - {{ option.title }}
      </option>
    </select>


  </div>
  <div>
    <label>title</label><input :text="title" type="text"/>
  </div>

  <div>
    <label>description</label><input :text="description" type="text"/>
  </div>
  <div>
    <label>photo</label>
    <ManagedFileComponent v-model:managed-file-id="photo"/>
  </div>
  <div>
    <label>introduction</label>
    <ManagedFileComponent v-model:managed-file-id="intro"/>
  </div>
  <div>
    <label>interview</label>
    <ManagedFileComponent v-model:managed-file-id="interview"/>
  </div>
</template>
