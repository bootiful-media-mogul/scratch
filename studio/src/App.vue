<!--
 renders a list of all the existing podcasts
-->
<script lang="ts">
import AiChatComponent from '@/ai/AiChatComponent.vue'

import { mogul } from '@/services'
import SidebarPanelComponent from '@/layout/SidebarPanelComponent.vue'
import PreviewComponent from '@/managedfiles/PreviewComponent.vue'

export default {
  components: { AiChatComponent, PreviewComponent, SidebarPanelComponent },

  methods: {},
  data() {
    const mogul = ''
    return {
      mogul
    }
  },
  async created() {
    this.mogul = await mogul.me()
  }
}
</script>
<style></style>
<template>
  <div class="frame">
    <div class="page">
      <div class="welcome">
        hello, <span style="font-weight: bold"> {{ mogul }} </span>!
      </div>

      <div class="toolbar">
        <router-link to="/">home</router-link>
        |
        <router-link to="/settings">settings</router-link>
        |
        <router-link to="/podcasts">podcasts</router-link>
      </div>

      <div class="view">
        <router-view></router-view>
      </div>

      <div class="sidebar">
        <SidebarPanelComponent title="A.I.">
          <AiChatComponent />
        </SidebarPanelComponent>

        <SidebarPanelComponent title="Media Preview">
          <PreviewComponent />
        </SidebarPanelComponent>

        <SidebarPanelComponent title="Notes"></SidebarPanelComponent>

        <SidebarPanelComponent title="Transcription"></SidebarPanelComponent>
      </div>
    </div>
    <div class="footer">
      Mogul, made with love ❤️ by
      <a href="https://youtube.com/@coffeesoftware">Josh Long</a> (and with Spring 🍃)
    </div>
  </div>
</template>
