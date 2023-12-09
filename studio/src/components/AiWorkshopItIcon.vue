<template>
  <a href="#" @click="workshop">workshop it!!</a>
</template>

<script lang="ts">
import { AiWorkshopReplyEvent, workshopInAi } from '@/services'

export default {
  emits: ['ai-workshop-completed'],
  props: ['text', 'prompt'],
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
      const prompt =
        this.prompt == null || this.prompt.trim() == ''
          ? this.text
          : this.prompt + '\n\n' + this.text
      workshopInAi(this.callbackFunction, prompt)
    }
  }
}
</script>
