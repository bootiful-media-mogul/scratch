<template>
  <a ref="link" class="ai-workshop-it-link" href="#" @click="workshop"> </a>
</template>

<style>
.ai-workshop-it-link {
  --icon-size: 3em;
  height: var(--icon-size);
  width: var(--icon-size);
  background: url('../assets/images/ai-icon.png');
  background-size: var(--icon-size) var(--icon-size);
  background-repeat: no-repeat;
  background-position: left;
}

.ai-workshop-it-link:hover {
  background: url('../assets/images/ai-icon-highlight.png');
  background-size: var(--icon-size) var(--icon-size);
  background-repeat: no-repeat;
  background-position: left;
}

label .ai-workshop-it-link {
  position: absolute;
  padding-top: var(--icon-size);
}
</style>

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
  mounted() {
    const us = this.$refs.link as HTMLElement
    const childElement = us.parentElement as HTMLElement
    const forElementIdName = childElement.getAttribute('for')
    if (forElementIdName != null && forElementIdName.toString().trim() != '') {
      const formElement = document.getElementById(forElementIdName) as HTMLElement
      const resizeFunction = () => {
        us.style.left = formElement.offsetWidth + us.offsetWidth + 10 + 'px'
      }
      new ResizeObserver(resizeFunction).observe(formElement)
      resizeFunction()
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
