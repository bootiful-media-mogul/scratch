<!--
 renders a list of all the existing podcasts
-->
<script lang="ts">
import AiChatComponent from '@/ai/AiChatComponent.vue'

import { mogul } from '@/services'
import SidebarPanelComponent from '@/layout/SidebarPanelComponent.vue'
import PreviewComponent from '@/managedfiles/PreviewComponent.vue'
import NotificationBox from '@/notifications/NotificationBox.vue'

export default {
  components: { NotificationBox, AiChatComponent, PreviewComponent, SidebarPanelComponent },

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

<!--
  where this renders depends on the kind of notification we get. so,
  im keeping it out of the main layout. it'll be display none until it isnt
  and when it isn't it'll display as a modal, dead center on the screen,
  or a toaster popup from the bottom of the screen. it'll be fixed positionally, either way
 -->
  <NotificationBox ref="notifications" />

  <div class="frame">
    <div class="page">
      <div class="welcome">
        {{ $t('hello') }} <span style="font-weight: bold"> {{ mogul }} </span>!
      </div>


      <div class="toolbar">
        <router-link to="/">{{ $t('app.menu.home') }}</router-link>
        |
        <router-link to="/settings">{{ $t('app.menu.settings') }}</router-link>
        |
        <router-link to="/podcasts">{{ $t('app.menu.podcasts') }}</router-link>
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
      <span
        v-html="$t('app.made-with-love', {'josh':'<a href=\'https://youtube.com/@coffeesoftware\'>Josh Long</a>'})"></span>
    </div>
  </div>
</template>
