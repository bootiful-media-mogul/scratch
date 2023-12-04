<!--
 renders a list of all the existing podcasts
-->
<script lang="ts">


import MogulClient from "@/mogulClient";

const api = new MogulClient();

export default {
  data() {
    return {mogul: ''}
  },
  async created() {
    this.mogul = await (await api.me())['data']['me'][ 'name']
    console.log(this.mogul)
  },
}
</script>

<template>
  <div>
    <div>
      hello, <b>{{ mogul }}</b>!
    </div>
    <div>
      <router-link to="/"> home</router-link>
      |
      <router-link to="/podcasts"> podcasts</router-link>
      <!--      |
            <router-link to="/youtube"> youtube videos</router-link>
            |
            <router-link to="/youtube"> blogs</router-link>-->
    </div>

    <div>
      <router-view></router-view>
    </div>
  </div>

</template>

<style scoped>
</style>
