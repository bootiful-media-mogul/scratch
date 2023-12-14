<!--
todo support workshopping the image for the podcast as well: give it
 a prompt and it renders an image which the code could store in a place
 to then be plugged in instead of a user submitted image
-->
<template>
  <form class="ai-workshop-panel pure-form pure-form-stacked">
    <fieldset>
      <legend>AI Workshop</legend>

      <label for="prompt">prompt</label>
      <textarea required id="prompt" rows="20" v-model="prompt"></textarea>

      <div v-for="reply in replies" :key="reply">
        {{ reply }}
        <button value="accept" @click="acceptSuggestion(reply, $event)">accept</button>
      </div>
      <div>
        <button
          :disabled="isPromptEmpty()"
          class="pure-button pure-button-primary"
          value="chat"
          @click="chat"
        >
          go
        </button>
        <button :disabled="isPromptEmpty()" class="pure-button" value="ok" @click="finished">
          finish
        </button>
      </div>
    </fieldset>
  </form>
</template>
<style>
.ai-workshop-panel textarea {
  width: 100%;
}

.ai-workshop-panel img {
  width: 50px;
}

.ai-workshop-panel fieldset legend {
  background-image: url('../assets/images/clippy.png');
  background-size: contain;
  background-clip: border-box;
  background-position-x: right;
  background-position-y: top;
  background-repeat: no-repeat;
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
  components: {},
  computed: {},
  methods: {
    acceptSuggestion(text: string, e: Event) {
      e.preventDefault()
      if (text.endsWith('"')) text = text.substring(0, text.length - 1)
      if (text.startsWith('"')) text = text.substring(1)
      this.prompt = text
      this.finished()
    },
    isPromptEmpty() {
      return (this.prompt == null ? '' : this.prompt).trim().length == 0
    },

    finished() {
      events.emit('sidebar-panel-closed', this.$el)
      this.callback(new AiWorkshopReplyEvent(this.prompt.trim(), AiWorkshopReplyEventType.TEXT))
      this.prompt = ''
      this.replies = []
    },

    async chat(event: Event) {
      event.preventDefault()
      const response = await ai.chat(this.prompt)
      this.replies.push(response)
    }
  },
  data() {
    return {
      prompt: '' as string,
      replies: [] as Array<string>,
      callback: function (arg0: AiWorkshopReplyEvent) {
        console.log('using the event ' + arg0)
        // noop
      }
    }
  },
  async created() {
    events.on('ai-workshop-event', (event) => {
      const aiEvent = event as AiWorkshopRequestEvent
      console.log('going to workshop the text [' + aiEvent.text + ']')
      this.prompt = aiEvent.text
      this.callback = aiEvent.callback
      this.replies = [] as Array<string>

      events.emit('sidebar-panel-opened', this.$el)
    })
  }
}
</script>
