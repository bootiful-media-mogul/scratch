<template>
  <div id="ai-chat-side-panel" class="panel">
    <div>
      <a href="#" @click="hide" v-if="visible">hide</a>
      <a href="#" @click="show" v-if="!visible">show</a>
    </div>
    <div v-if="visible">
      <h3>ai workshop</h3>
      <div><a href="">chat</a> | <a href="">render</a> | <a href="">transcribe</a></div>
      <textarea rows="20" v-model="prompt"></textarea>
      <div v-for="reply in replies" :key="reply">
        {{ reply }}
      </div>
      <button value="chat" @click="chat">chat</button>
    </div>
  </div>
</template>

<style>
.panel {
  background-color: aliceblue;
  padding: calc(var(--gutter-space));
  border-radius: 10px 0px 0px 10px;
  right: calc(-1 * var(--gutter-space));
}
</style>

<script lang="ts">
import { events, ai, AiWorkshopEvent } from '@/services'


export default {
  methods: {
    hide() {
      this.visible = false
    },
    show() {
      this.visible = true
    },
    async chat(event: Event) {
      event.preventDefault()
      const response = await ai.chat(this.prompt)
      this.replies.push(response)
    }
  },
  data() {
    return {
      visible: false,
      prompt: '' as string,
      replies: [] as Array<string>
    }
  },
  created: async function() {
    events.on('ai-workshop-event', (event) => {
      const aiEvent = event as AiWorkshopEvent
      console.log('going to workshop the text [' + aiEvent.text + ']')
      this.prompt = aiEvent.text
      this.visible = true
    })
  }
}
</script>
