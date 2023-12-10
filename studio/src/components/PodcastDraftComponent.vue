<template>
  <hr />
  <h3>
    <span v-if="podcast.uid"> {{ podcast.uid }} </span>
    <span v-if="podcast.title"> {{ podcast.title }}</span>
  </h3>
  <div>{{ buildPresentableDate(podcast.date) }}</div>
  <div>
    <div v-html="podcast.description"></div>
  </div>
  <hr />
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
