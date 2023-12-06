<!--
 renders a list of all the existing podcasts
-->
<script lang="ts">
import MogulClient from '@/mogulClient'

const api = new MogulClient()

export default {
  data() {
    return { mogul: '' }
  },
  async created() {
    this.mogul = await api.me()
  }
}
</script>

<template>
  <div>
    <div>
      hello, <b>{{ mogul }}</b
      >!
    </div>
    <div>
      <router-link to="/"> home</router-link>
      |
      <router-link to="/list-podcasts"> list podcasts</router-link>
      |
      <router-link to="/create-podcast"> create podcast</router-link>
    </div>
    <hr />
    <div>
      <router-view></router-view>
    </div>
  </div>
</template>

<style scoped></style>
