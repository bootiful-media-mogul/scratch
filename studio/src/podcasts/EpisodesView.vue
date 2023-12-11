<script lang="ts">
import { defineComponent, ref } from 'vue'
import ManagedFileComponent from '@/managedfiles/ManagedFileComponent.vue'
import { ManagedFile, Podcast, podcasts } from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'

export default defineComponent({
  components: { AiWorkshopItIconComponent, ManagedFileComponent },

  props: ['podcastId'],

  async beforeMount() {
    const possiblePodcasts = await podcasts.podcasts()
    for (let i = 0; i < possiblePodcasts.length; i++) this.podcasts.push(possiblePodcasts[i])

    this.podcast = this.podcasts.filter((p) => p.id == this.podcastId)[0]
    console.log('working with ' + JSON.stringify(this.podcast))

    this.$forceUpdate()
  },
  setup() {
    return {
      managedFile: null as any as ManagedFile,
      podcast: null as any as Podcast,
      podcasts: [] as Array<Podcast>,
      title: '',
      description: '',
      intro: ref(null as any),
      interview: ref(null as any),
      photo: ref(null as any)
    }
  }
})
</script>
<template>
  <h1>Episodes</h1>
  <form class="pure-form pure-form-aligned">
    <fieldset>
      <legend v-if="podcast">Create a New Podcast Episode for "{{ podcast.title }}"</legend>

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
        <input required :text="title" type="text" />
        <AiWorkshopItIconComponent
          prompt="please help me take the following podcast title and make it more pithy and exciting"
          :text="title"
          @ai-workshop-completed="title = $event.text"
        />
        <!--        <span class="pure-form-message-inline">
                This is a required field
                </span>-->
      </div>
      <div class="pure-control-group">
        <label>description</label>
        <textarea required :text="description" type="text" />
        <AiWorkshopItIconComponent
          prompt="please help me take the following podcast title and make it more pithy and exciting"
          :text="title"
          @ai-workshop-completed="title = $event.text"
        />
      </div>
      <div class="pure-control-group">
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
      </div>
    </fieldset>
  </form>
</template>
