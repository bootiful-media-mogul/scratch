<script lang="ts">

export default {
  data() {
    return {mogul: ''}
  },
  async created() {
    this.mogul = (await (await window.fetch('/api/me')).json()) ['name']
  },
}
</script>

<template>
  <div>
    <div>
      hello {{ mogul }}
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
