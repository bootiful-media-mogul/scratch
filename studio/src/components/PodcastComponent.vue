<template>
  <h3>
    {{ podcast.title }}
  </h3>
  <div>{{ buildPresentableDate(podcast.date) }}</div>
  <div>
    <div v-html="podcast.html"></div>
  </div>
  <div>
    <a href="#" @click.prevent="$emit('episodeDeleted')">delete</a>
  </div>
</template>

<script lang="ts">
import { Podcast } from '@/model'
import { dateTimeFormatter } from '@/dates'

export default {
  emits: ['episodeDeleted'],
  props: {
    podcast: {
      type: Object as () => Podcast,
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
