<!--
todo support workshopping the image for the podcast as well: give it
 a prompt and it renders an image which the code could store in a place
 to then be plugged in instead of a user submitted image

-->
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
        <button value="accept" @click="acceptSuggestion(reply ,$event)">accept</button>
      </div>
      <button :disabled="isPromptEmpty()" value="chat" @click="chat">workshop it!</button>
      <button :disabled="isPromptEmpty()" value="ok" @click="finished">finish</button>
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
import {
  ai,
  AiWorkshopReplyEvent,
  AiWorkshopReplyEventType,
  AiWorkshopRequestEvent,
  events
} from '@/services'

export default {
  methods: {
    acceptSuggestion(text: string, e: Event) {

      e.preventDefault()

      console.log('going to accept ' + text)
      if (text.endsWith('"')) text = text.substring(0, text.length - 1)
      if (text.startsWith('"')) text = text.substring(1);
      this.prompt = text
      console.log('ended up with [' + this.prompt + ']')
      this.finished()
    },
    isPromptEmpty() {
      return (this.prompt == null ? '' : this.prompt).trim().length == 0
    },
    hide() {
      this.visible = false
    },
    finished() {
      console.log('finishing...')
      this.hide()
      this.callback(new AiWorkshopReplyEvent(this.prompt.trim(), AiWorkshopReplyEventType.TEXT))
      this.prompt = ''
      this.replies = []
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
      replies: [] as Array<string>,
      callback: function (arg0: AiWorkshopReplyEvent) {
        // noop
      }
    }
  },
  async created() {
    events.on('ai-workshop-event', (event) => {
      const aiEvent = event as AiWorkshopRequestEvent
      console.log('going to workshop the text [' + aiEvent.text + ']')
      this.prompt = aiEvent.text
      this.visible = true
      this.callback = aiEvent.callback
      this.replies = [] as Array<string>
    })
  }
}
</script>
