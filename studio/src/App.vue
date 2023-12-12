<!--
 renders a list of all the existing podcasts
-->
<script lang="ts">
import AiChatComponent from '@/ai/AiChatComponent.vue'

import { mogul } from '@/services'
import SidebarPanelComponent from '@/layout/SidebarPanelComponent.vue'

export default {
  components: { AiChatComponent, SidebarPanelComponent },

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

<template>
  <div class="page">
    <div class="welcome">
      hello, <span style="font-weight: bold"> {{ mogul }} </span>!
    </div>

    <div class="toolbar">
      <router-link to="/">home</router-link>
      |
      <router-link to="/podcasts">podcasts</router-link>
    </div>

    <div class="view">
      <router-view></router-view>
    </div>

    <div class="sidebar">
      <SidebarPanelComponent title="AI">
        <AiChatComponent />
      </SidebarPanelComponent>

      <SidebarPanelComponent title="Notes"> </SidebarPanelComponent>
    </div>
  </div>
</template>
