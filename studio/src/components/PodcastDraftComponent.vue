<template>


  <h3>
    {{ podcast.title }}
  </h3>
  <div> {{ buildPresentableDate(podcast.date) }}</div>
  <div>
    <div v-html="podcast.description"></div>
  </div>

</template>

<script lang="ts">
import { PodcastDraft } from '@/model'
import { dateTimeFormatter } from '@/dates'

export default {
  props: {
    podcast: {
      type: Object as () => PodcastDraft,
      required: true
    }
  },
  methods: {

    buildPresentableDate(existingDate: Date) {
      return dateTimeFormatter.format(existingDate)
    }
  }


}
</script>
