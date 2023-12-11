<!--
 renders a list of all the existing podcasts
-->
<script lang="ts">
import AiChat from '@/ai/AiChatComponent.vue'

import { mogul } from '@/services'

export default {
  components: { AiChat },

  methods: {},
  data() {
    return {
      mogul: ''
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
      hello,
      <span style="font-weight: bold"> {{ mogul }} </span>!
    </div>
    <div class="toolbar">
      <div>
        <router-link to="/">home</router-link>
      </div>
      <div>

        <router-link to="/podcasts">podcasts</router-link>
        <!--        <router-link to="/create-podcast">create </router-link>&nbsp;-->
      </div>
      <div>
        <b>blogs</b>&nbsp;
        <a href="">create</a>
      </div>
      <div>
        <b>videos </b>&nbsp;
        <a href="">create</a>
      </div>
    </div>

    <div class="view">
      <router-view></router-view>
    </div>

    <div class="sidebar">
      <AiChat />
    </div>
  </div>
</template>

<style scoped></style>
