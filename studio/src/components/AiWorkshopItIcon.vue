<template>
  <a href="#" @click="workshop">workshop it!!</a>
</template>

<script lang="ts">
import { AiWorkshopReplyEvent, workshopInAi } from '@/services'

export default {
  emits: ['ai-workshop-completed'],
  props: ['text'],
  data() {
    return {
      description: ''
    }
  },

  setup(props, ctx) {
    return {
      callbackFunction: (updated: AiWorkshopReplyEvent) =>
        ctx.emit('ai-workshop-completed', updated)
    }
  },

  methods: {
    workshop(e: Event) {
      e.preventDefault()
      workshopInAi(this.callbackFunction, this.text)
    }
  }
}
</script>
